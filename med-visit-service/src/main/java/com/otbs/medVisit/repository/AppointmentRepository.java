package com.otbs.medVisit.repository;

import com.otbs.medVisit.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    //findByPatientId
    List<Appointment> findByEmployeeId(String employeeId);
    boolean existsByMedicalVisitId(Long medicalVisitId);
    List<Appointment> findByMedicalVisitId(Long medicalVisitId);
}
