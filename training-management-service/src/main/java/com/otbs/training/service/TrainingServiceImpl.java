package com.otbs.training.service;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.training.dto.TrainingRequestDTO;
import com.otbs.training.dto.TrainingResponseDTO;
import com.otbs.training.exception.TrainingException;
import com.otbs.training.mapper.TrainingMapper;
import com.otbs.training.model.EStatus;
import com.otbs.training.model.Invitation;
import com.otbs.training.model.Training;
import com.otbs.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRepository trainingRepository;
    private final TrainingMapper trainingMapper;
    private final EmployeeClient employeeClient;
    private final TrainingNotificationService trainingNotificationService;

    @Override
    @Transactional
    public void createTraining(TrainingRequestDTO request) {
        String managerId = getCurrentUserId();
        EmployeeResponse manager = employeeClient.getEmployeeByDn(managerId);

        validateDateRange(request);

        Training training = trainingMapper.toEntity(request);
        training.setCreatedBy(managerId);
        training.setDepartment(manager.department());

        Training savedTraining = trainingRepository.save(training);

        List<EmployeeResponse> departmentEmployees = employeeClient.getAllEmployees().stream()
                .filter(emp -> emp.department().equals(manager.department()))
                .filter(emp -> !emp.equals(manager))
                .toList();

        List<Invitation> invitations = departmentEmployees.stream()
                .map(emp -> {
                    Invitation invitation = new Invitation();
                    invitation.setEmployeeId(emp.id());
                    invitation.setStatus(EStatus.PENDING);
                    invitation.setTraining(savedTraining);
                    trainingNotificationService.sendTrainingNotification(savedTraining.getId(), emp.username());
                    return invitation;
                })
                .collect(Collectors.toList());

        savedTraining.setInvitations(invitations);
        trainingRepository.save(savedTraining);
    }

    @Override
    @Transactional
    public void updateTraining(TrainingRequestDTO request, Long trainingId) {
        String managerId = getCurrentUserId();
        Training training = findTrainingForManager(managerId, trainingId);

        validateDateRange(request);

        training.setTitle(request.title());
        training.setDescription(request.description());
        training.setStartDate(request.startDate());
        training.setEndDate(request.endDate());

        trainingRepository.save(training);
    }

    @Override
    @Transactional
    public void deleteTraining(Long trainingId) {
        String managerId = getCurrentUserId();
        Training training = findTrainingForManager(managerId, trainingId);
        trainingRepository.delete(training);
    }

    @Override
    public TrainingResponseDTO getTrainingById(Long trainingId) {
        String userId = getCurrentUserId();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userId);

        if ("HR".equals(user.role())) {
            return trainingMapper.toResponseDTO(trainingRepository.findById(trainingId)
                    .orElseThrow(() -> new TrainingException("Training not found")));
        } else if ("Manager".equals(user.role())) {
            return trainingMapper.toResponseDTO(findTrainingForManager(userId, trainingId));
        }

        throw new TrainingException("Not authorized to view this training");
    }

    @Override
    public List<TrainingResponseDTO> getAllTrainings() {
        String userId = getCurrentUserId();
        EmployeeResponse user = employeeClient.getEmployeeByDn(userId);

        if ("HR".equals(user.role())) {
            return trainingRepository.findAll().stream()
                    .map(trainingMapper::toResponseDTO)
                    .toList();
        } else if ("Manager".equals(user.role())) {
            return trainingRepository.findByCreatedBy(userId).stream()
                    .map(trainingMapper::toResponseDTO)
                    .toList();
        }else if ("Employee".equals(user.role())) {
            return trainingRepository.findByInvitations_EmployeeId(userId).stream()
                    .peek(training -> training.setInvitations(training.getInvitations().stream()
                            .filter(invitation -> invitation.getEmployeeId().equals(userId))
                            .collect(Collectors.toList())))
                    .map(trainingMapper::toResponseDTO)
                    .toList();
        }

        throw new TrainingException("Not authorized to view trainings");
    }


    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Training findTrainingForManager(String managerId, Long trainingId) {
        return trainingRepository.findByCreatedByAndId(managerId, trainingId)
                .orElseThrow(() -> new TrainingException("Training not found or not authorized"));
    }

    private void validateDateRange(TrainingRequestDTO request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new TrainingException("Start date must be before end date");
        }
    }
}