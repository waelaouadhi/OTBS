package com.otbs.employee.repository;

import com.otbs.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeInfoRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByUsername(String username);
}
