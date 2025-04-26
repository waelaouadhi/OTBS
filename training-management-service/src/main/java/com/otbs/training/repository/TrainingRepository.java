package com.otbs.training.repository;

import com.otbs.training.model.Training;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    Optional<Training> findByCreatedByAndId(String createdBy, Long id);
    List<Training> findByCreatedBy(String createdBy);
    List<Training> findByInvitations_EmployeeId(String employeeId);
}
