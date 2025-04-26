package com.otbs.training.repository;

import com.otbs.training.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository  extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByEmployeeIdAndTrainingId(String employeeId, Long trainingId);
    List<Invitation> findByEmployeeId(String employeeId);
}
