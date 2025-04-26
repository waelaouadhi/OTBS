package com.otbs.medVisit.service;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.medVisit.dto.AppointmentRequest;
import com.otbs.medVisit.dto.AppointmentResponse;
import com.otbs.medVisit.dto.MedicalVisitResponse;
import com.otbs.medVisit.exception.AppointmentException;
import com.otbs.medVisit.exception.MedicalVisitException;
import com.otbs.medVisit.exception.TimeSlotNotAvailableException;
import com.otbs.medVisit.mapper.AppointmentMapper;
import com.otbs.medVisit.model.Appointment;
import com.otbs.medVisit.model.EAppointmentStatus;
import com.otbs.medVisit.repository.AppointmentRepository;
import com.otbs.medVisit.repository.MedicalVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalVisitService medicalVisitService;
    private final MedicalVisitRepository medicalVisitRepository;
    private final AppointmentMapper appointmentMapper;
    private final EmployeeClient employeeClient;

    @Override
    public void createAppointment(AppointmentRequest appointment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //verify if employee has already an appointment
        if (appointmentRepository.findAll().stream().anyMatch(
                app -> {
                    return app.getEmployeeId().equals(authentication.getName())&&app.getMedicalVisit().getId().equals(appointment.medicalVisitId());
                }
        )
        ) {
            throw new AppointmentException("You already have an appointment");
        }

        //verify if the medical visit not old or not exist
        if (medicalVisitService.getMedicalVisit(appointment.medicalVisitId()).visitDate().isBefore(LocalDate.now())) {
            throw new MedicalVisitException("Medical visit is not available");
        }

        if (isInvalidTimeSlot(appointment)) {
            throw new TimeSlotNotAvailableException("Time slot should be between MedicalVisit start and end time for every 30 minutes");
        }

        if (isTimeSlotTaken(appointment)) {
            throw new TimeSlotNotAvailableException("Time slot not available");
        }


        medicalVisitRepository.findById(appointment.medicalVisitId()).ifPresentOrElse(medicalVisit -> {
            Appointment newAppointment = Appointment.builder()
                    .employeeId(authentication.getName())
                    .medicalVisit(medicalVisit)
                    .timeSlot(appointment.timeSlot())
                    .status(EAppointmentStatus.PLANIFIE)
                    .build();
            appointmentRepository.save(newAppointment);
        }, () -> {
            throw new AppointmentException("Medical visit not found");
        });
    }

    @Override
    public void updateAppointment(AppointmentRequest appointmentRequest, Long appointmentId) {

        if (!appointmentRepository.existsByMedicalVisitId(appointmentRequest.medicalVisitId())) {
            throw new AppointmentException("No appointment found for this medical visit");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HR")) && !isAppointmentMine(appointmentId)) {
            throw new AppointmentException("You are not allowed to update this appointment");
        }


        appointmentRepository.findById(appointmentId).ifPresentOrElse(appointment -> {
            if (isInvalidTimeSlot(appointmentRequest, appointment)) {
                throw new TimeSlotNotAvailableException("Time slot should be between MedicalVisit start and end time for every 30 minutes");
            }
            if (isTimeSlotTaken(appointmentRequest)) {
                throw new TimeSlotNotAvailableException("Time slot not available");
            }
            appointment.setTimeSlot(appointmentRequest.timeSlot());
            appointmentRepository.save(appointment);
        }, () -> {
            throw new AppointmentException("Appointment not found");
        });
    }

    @Override
    public void deleteAppointment(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HR")) && !isAppointmentMine(id)) {
            throw new AppointmentException("You are not allowed to delete this appointment");
        }
        appointmentRepository.deleteById(id);
    }

    @Override
    public AppointmentResponse getAppointmentById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HR")) && !isAppointmentMine(id)) {
            throw new AppointmentException("You are not allowed to view this appointment");
        }
        return appointmentRepository.findById(id)
                .map(appointment ->
                {
                    String employeeFullName = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).firstName()+" "+employeeClient.getEmployeeByDn(appointment.getEmployeeId()).lastName();
                    String employeeEmail = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).email();
                    return appointmentMapper.toDto(appointment, employeeFullName, employeeEmail);
                }
                )
                .orElseThrow(() -> new AppointmentException("Appointment not found"));
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HR"))) {
            return appointmentRepository.findByEmployeeId(authentication.getName()).stream()
                    .map(appointment -> {
                        String employeeFullName = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).firstName()+" "+employeeClient.getEmployeeByDn(appointment.getEmployeeId()).lastName();
                        String employeeEmail = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).email();
                        return appointmentMapper.toDto(appointment, employeeFullName, employeeEmail);
                    })
                    .toList();
        }

        return appointmentRepository.findAll().stream()
                .map(appointment -> {
                    EmployeeResponse employee = employeeClient.getEmployeeByDn(appointment.getEmployeeId());
                    String employeeFullName = employee.firstName()+" "+employee.lastName();
                    String employeeEmail = employee.email();
                    return appointmentMapper.toDto(appointment, employeeFullName, employeeEmail);
                })
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByPatientId(String patientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("HR")) && !authentication.getName().equals(patientId)) {
            throw new AppointmentException("You are not allowed to view those appointments");
        }
        return appointmentRepository.findByEmployeeId(patientId).stream()
                .map(appointment -> {
                    String employeeFullName = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).firstName()+" "+employeeClient.getEmployeeByDn(appointment.getEmployeeId()).lastName();
                    String employeeEmail = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).email();
                    return appointmentMapper.toDto(appointment, employeeFullName, employeeEmail);
                })
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByMedVisitId(String medVisitId) {
        return appointmentRepository.findByMedicalVisitId(Long.parseLong(medVisitId)).stream()
                .map(appointment -> {
                    String employeeFullName = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).firstName()+" "+employeeClient.getEmployeeByDn(appointment.getEmployeeId()).lastName();
                    String employeeEmail = employeeClient.getEmployeeByDn(appointment.getEmployeeId()).email();
                    return appointmentMapper.toDto(appointment, employeeFullName, employeeEmail);
                })
                .toList();
    }

    private boolean isInvalidTimeSlot(AppointmentRequest appointmentRequest, Appointment appointment) {
        return appointmentRequest.timeSlot().isBefore(LocalDateTime.of(appointment.getMedicalVisit().getVisitDate(), appointment.getMedicalVisit().getStartTime())) ||
                appointmentRequest.timeSlot().isAfter(LocalDateTime.of(appointment.getMedicalVisit().getVisitDate(), appointment.getMedicalVisit().getEndTime())) ||
                appointmentRequest.timeSlot().getMinute() % 30 != 0;
    }

    private boolean isInvalidTimeSlot(AppointmentRequest appointmentRequest) {
        MedicalVisitResponse medicalVisit = medicalVisitService.getMedicalVisit(appointmentRequest.medicalVisitId());
        return appointmentRequest.timeSlot().isBefore(LocalDateTime.of(medicalVisit.visitDate(), medicalVisit.startTime())) ||
                appointmentRequest.timeSlot().isAfter(LocalDateTime.of(medicalVisit.visitDate(), medicalVisit.endTime())) ||
                appointmentRequest.timeSlot().getMinute() % 30 != 0;
    }

    private boolean isTimeSlotTaken(AppointmentRequest appointmentRequest) {
        return appointmentRepository.findAll().stream()
                .anyMatch(appointment -> appointment.getTimeSlot().equals(appointmentRequest.timeSlot()));
    }

    //verify if appointment is mine
    private boolean isAppointmentMine(Long appointmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> appointment.getEmployeeId().equals(authentication.getName()))
                .orElse(false);
    }
}
