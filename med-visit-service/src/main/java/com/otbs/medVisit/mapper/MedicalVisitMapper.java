package com.otbs.medVisit.mapper;

import com.otbs.medVisit.dto.MedicalVisitRequest;
import com.otbs.medVisit.dto.MedicalVisitResponse;
import com.otbs.medVisit.model.MedicalVisit;
import org.springframework.stereotype.Service;


@Service
public class MedicalVisitMapper {
    public MedicalVisit toEntity(MedicalVisitRequest medicalVisitRequest) {
        return new MedicalVisit(
                medicalVisitRequest.doctorName(),
                medicalVisitRequest.visitDate(),
                medicalVisitRequest.startTime(),
                medicalVisitRequest.endTime()
        );
    }

    public MedicalVisitResponse toDto(MedicalVisit medicalVisit) {
        return new MedicalVisitResponse(
                medicalVisit.getId(),
                medicalVisit.getDoctorName(),
                medicalVisit.getVisitDate(),
                medicalVisit.getStartTime(),
                medicalVisit.getEndTime(),
                medicalVisit.getAppointments().size()
        );
    }
}
