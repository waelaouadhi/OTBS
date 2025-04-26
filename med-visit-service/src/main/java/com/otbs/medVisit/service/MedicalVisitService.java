package com.otbs.medVisit.service;

import com.otbs.medVisit.dto.MedicalVisitRequest;
import com.otbs.medVisit.dto.MedicalVisitResponse;

import java.util.List;

public interface MedicalVisitService {
    void createMedicalVisit(MedicalVisitRequest medicalVisitRequest);
    void updateMedicalVisit(MedicalVisitRequest medicalVisitRequest, Long medicalVisitId);
    void deleteMedicalVisit(Long id);
    MedicalVisitResponse getMedicalVisit(Long id);
    List<MedicalVisitResponse> getMedicalVisits();
}
