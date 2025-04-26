package com.otbs.leave.repository;

import com.otbs.leave.model.Leave;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findAllByUserDn(String userDn);
    Page<Leave> findAllByUserDn(String userDn, Pageable pageable);
}
