package com.otbs.medVisit.service;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.medVisit.dto.MedicalVisitRequest;
import com.otbs.medVisit.dto.MedicalVisitResponse;
import com.otbs.medVisit.exception.MedicalVisitException;
import com.otbs.medVisit.mapper.MedicalVisitMapper;
import com.otbs.medVisit.model.MedicalVisit;
import com.otbs.medVisit.repository.MedicalVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MedicalVisitServiceImpl implements MedicalVisitService{

    private final MedicalVisitRepository medicalVisitRepository;
    private final MedicalVisitMapper medicalVisitMapper;
    private final EmployeeClient employeeClient;
    private final MedicalVisitNotificationService notificationService;


    @Override
    public void createMedicalVisit(MedicalVisitRequest medicalVisitRequest) {

        final MedicalVisit medVisit = medicalVisitMapper.toEntity(medicalVisitRequest);
        //verify if medical visit already exists for the same doctor and date
        medicalVisitRepository.findByDoctorNameAndVisitDate(medicalVisitRequest.doctorName(), medicalVisitRequest.visitDate())
                .ifPresentOrElse(
                        existingVisit -> {
                            throw new MedicalVisitException("Medical visit already exists for the same doctor and date");
                        },
                        () -> {
                            medicalVisitRepository.save(medVisit);
                        }
                );

        employeeClient.getAllEmployees()
                .forEach(employee -> {
                            notificationService.sendMedicalVisitNotification(
                                    "New Medical Visit Scheduled",
                                    medicalVisitRequest.visitDate().toString(),
                                    employee.username()
                            );
                }

                );
    }

    @Override
    public void updateMedicalVisit(MedicalVisitRequest medicalVisitRequest, Long medicalVisitId) {
        medicalVisitRepository.findById(medicalVisitId)
                .ifPresentOrElse(
                        medicalVisit -> {
                            medicalVisit.setDoctorName(medicalVisitRequest.doctorName());
                            medicalVisit.setVisitDate(medicalVisitRequest.visitDate());
                            medicalVisit.setStartTime(medicalVisitRequest.startTime());
                            medicalVisit.setEndTime(medicalVisitRequest.endTime());
                            medicalVisitRepository.save(medicalVisit);
                        },
                        () -> {
                            throw new MedicalVisitException("Medical visit not found");
                        }
                );
    }

    @Override
    public void deleteMedicalVisit(Long id) {
        medicalVisitRepository.deleteById(id);
    }

    @Override
    public MedicalVisitResponse getMedicalVisit(Long id) {
        return medicalVisitRepository.findById(id)
                .map(
                        medicalVisitMapper::toDto
                )
                .orElseThrow(
                        () -> new MedicalVisitException("Medical visit not found")
                );
    }

    @Override
    public List<MedicalVisitResponse> getMedicalVisits() {
        return medicalVisitRepository.findAll().stream()
                .map(
                        medicalVisitMapper::toDto
                )
                .toList();
    }
}
