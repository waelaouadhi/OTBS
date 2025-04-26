package com.otbs.leave.service;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.client.MailClient;
import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.feign.dto.MailRequest;
import com.otbs.feign.dto.MailResponse;
import com.otbs.leave.dto.LeaveRequest;
import com.otbs.leave.dto.LeaveResponse;
import com.otbs.leave.exception.*;
import com.otbs.leave.mapper.LeaveAttributesMapper;
import com.otbs.leave.model.*;
import com.otbs.leave.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveNotificationService leaveNotificationService;
    private final LeaveAttributesMapper leaveAttributesMapper;
    private final MailClient mailClient;
    private final EmployeeClient employeeClient;
    @Override
    public void applyLeave(LeaveRequest leaveRequest, MultipartFile attachment) {
        Leave leave = leaveAttributesMapper.toEntity(leaveRequest);
        String userDn = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userDn);
        leave.setUserDn(user.id());

        //Upload attachment if present
        if (attachment != null && !attachment.isEmpty()) {
            try {
                leave.setAttachment(attachment.getBytes());
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload attachment");
            }
        }


        //Validate leave date range
        leaveRepository.findAllByUserDn(leave.getUserDn()).forEach(existingLeave -> {
            if (existingLeave.getStatus().equals(EStatus.APPROUVÉE) || existingLeave.getStatus().equals(EStatus.EN_ATTENTE)) {
                if (leave.getStartDate().isBefore(existingLeave.getEndDate()) && leave.getEndDate().isAfter(existingLeave.getStartDate())) {
                    throw new InvalidTimeRangeException("Leave date range is overlapping with existing leave");
                }
            }
        });


        leaveRepository.saveAndFlush(leave);




        //send mail to user
        MailResponse c = mailClient.sendMail(new MailRequest(user.email(), "Leave Application", "Your leave application has been submitted successfully"));
        log.info("Mail sent to user: {}", c.message());

        //send mail to manager
        EmployeeResponse manager = employeeClient.getManagerByDepartment(user.department());
        MailResponse c1 = mailClient.sendMail(new MailRequest(manager.email(), "Leave Application", "You have a new leave application to approve"));
        log.info("Mail sent to manager: {}", c1.message());

        //TODO: Send Notification to Manager
        leaveNotificationService.sendMedicalVisitNotification(
                "New Leave Request Received",
                leave.getStartDate().toString(),
                manager.username()
        );
    }

    @Override
    public void cancelLeave(Long leaveId) {
        leaveRepository.deleteById(leaveId);
        String userDn = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userDn);
        mailClient.sendMail(new MailRequest(user.email(), "Leave Application", "Your leave application has been cancelled successfully"));
    }

    @Override
    public void approveLeave(Long leaveId) {
        leaveRepository.findById(leaveId).ifPresentOrElse(leave -> {
            leave.setStatus(EStatus.APPROUVÉE);
            leaveRepository.save(leave);
            leaveBalanceRepository.findByUserDn(leave.getUserDn()).ifPresentOrElse(leaveBalance -> {
                int daysBetween = (int) ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate());
                leaveBalance.setUsedLeave(leaveBalance.getUsedLeave() + daysBetween);
                leaveBalance.setRemainingLeave(leaveBalance.getTotalLeave() - leaveBalance.getUsedLeave());
                leaveBalanceRepository.save(leaveBalance);
            }, () -> {
                throw new LeaveBalanceNotFoundException("Leave balance not found");
            });

            EmployeeResponse user = employeeClient.getEmployeeByDn(leave.getUserDn());
            mailClient.sendMail(new MailRequest(user.email(), "Leave Application", "Your leave application has been approved successfully"));
            //TODO: Send Notification to HR
            //TODO: Send Notification to Employee
        }, () -> {
            throw new LeaveNotFoundException("Leave not found");
        });
    }

    @Override
    public void rejectLeave(Long leaveId) {
        leaveRepository.findById(leaveId).ifPresentOrElse(leave -> {
            leave.setStatus(EStatus.REFUSÉE);
            leaveRepository.save(leave);

            EmployeeResponse user = employeeClient.getEmployeeByDn(leave.getUserDn());
            mailClient.sendMail(new MailRequest(user.email(), "Leave Application", "Your leave application has been rejected successfully"));
            //TODO: Send Notification to HR
            //TODO: Send Notification to Employee
        }, () -> {
            throw new LeaveNotFoundException("Leave not found");
        });
    }

    @Override
    public List<Leave> getLeaveHistory(String userDn) {
        return leaveRepository.findAllByUserDn(userDn);
    }

    @Override
    public LeaveBalance getLeaveBalance(String userDn) {
        return leaveBalanceRepository.findByUserDn(userDn).orElseThrow(() -> new LeaveBalanceNotFoundException("Leave balance not found"));
    }


    @Override
    public List<LeaveResponse> getAllLeaves() {
        String userDn = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userDn);
        if(user.role().equals("Manager")){
            return leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().filter(leave -> {
                EmployeeResponse employee = employeeClient.getEmployeeByDn(leave.getUserDn());
                return employee != null && employee.department() != null && employee.department().equals(user.department());
            }).map(leaveAttributesMapper::toDto).collect(Collectors.toList());
        }

        return leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(leaveAttributesMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<Leave> getAllLeavesForManager(Pageable pageable) {
        String userDn = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userDn);
        if (user == null || user.department() == null) {
            throw new IllegalStateException("User or user department is null");
        }
        List<Leave> filteredLeaves = leaveRepository.findAll(pageable).stream().filter(leave -> {
            EmployeeResponse employee = employeeClient.getEmployeeByDn(leave.getUserDn());
            return employee != null && employee.department() != null && employee.department().equals(user.department());
        }).collect(Collectors.toList());
        return new PageImpl<>(filteredLeaves, pageable, filteredLeaves.size());
    }

    @Override
    public void updateLeave(Long leaveId, LeaveRequest leaveRequest) {
        leaveRepository.findById(leaveId).ifPresentOrElse(leave -> {
            leaveAttributesMapper.updateEntity(leave, leaveRequest);
            leaveRepository.save(leave);
        }, () -> {
            throw new LeaveNotFoundException("Leave not found");
        });
    }

    @Override
    public byte[] downloadAttachment(Long leaveId) {
        String userDn = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userDn);
        Optional<Leave> attachment = leaveRepository.findById(leaveId);
        if ("HR".equals(user.department())) {
            return attachment.map(Leave::getAttachment).orElseThrow(() -> new AttachmentNotFoundException("Attachment not found"));
        }
        return attachment.filter(leave -> {
            EmployeeResponse employee = employeeClient.getEmployeeByDn(leave.getUserDn());
            return employee.department().equals(user.department());
        }).map(Leave::getAttachment).orElseThrow(() -> new LeaveNotFoundException("Leave not found"));
    }

    @Override
    @Scheduled(cron = "0 0 0 1 * *")
    public void addMonthlyLeaveForAllEmployees() {
        leaveBalanceRepository.findAll().forEach(leaveBalance -> {
            leaveBalance.addMonthlyLeave();
            leaveBalanceRepository.save(leaveBalance);
        });
    }
}