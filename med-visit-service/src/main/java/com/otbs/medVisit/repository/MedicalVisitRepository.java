package com.otbs.medVisit.repository;

import com.otbs.medVisit.model.MedicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    Optional<MedicalVisit> findByDoctorNameAndVisitDate(String doctorName, LocalDate visitDate);

}