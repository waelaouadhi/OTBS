package com.otbs.employee.service;

import com.otbs.employee.dto.EmployeeInfoRequest;
import com.otbs.employee.dto.ProfilePicture;
import com.otbs.employee.exception.EmployeeNotFoundException;
import com.otbs.employee.exception.FileUploadException;
import com.otbs.employee.mapper.UserAttributesMapper;
import com.otbs.employee.model.Employee;
import com.otbs.employee.repository.EmployeeInfoRepository;
import com.otbs.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.Name;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeRepository employeeRepository;
    private final UserAttributesMapper userAttributesMapper;
    private final EmployeeInfoRepository employeeInfoRepository;

    @Override
    public Employee getEmployeeByDn(Name dn) {
        return employeeRepository.findById(dn)
                .map(userAttributesMapper)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
    }

    @Override
    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .map(userAttributesMapper)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(userAttributesMapper)
                .filter(employee -> !employee.getDepartment().equals("Unknown")
                        && !employee.getDepartment().equals("Domain Controllers"))
                .toList();
    }

    @Override
    public Employee getEmployeeByUsername(String username) {
        Employee employee = employeeRepository.findByUsername(username)
                .map(userAttributesMapper)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));

        employeeInfoRepository.findById(employee.getId().toString())
                .ifPresentOrElse(
                        info -> {
                        employee.setFirstName(info.getFirstName());
                        employee.setLastName(info.getLastName());
                        employee.setEmail(info.getEmail());
                        employee.setPicture(info.getPicture());
                        employee.setPictureType(info.getPictureType());
                        employee.setJobTitle(info.getJobTitle());
                        },
                        () -> {
                            Employee employeeInfo = Employee.builder()
                                    .id(employee.getId())
                                    .username(employee.getUsername())
                                    .firstName(employee.getFirstName())
                                    .lastName(employee.getLastName())
                                    .email(employee.getEmail())
                                    .department(employee.getDepartment())
                                    .role(employee.getRole())
                                    .build();
                            employeeInfoRepository.save(employeeInfo);
                        }
                );

        return employee;
    }

    @Override
    public Employee getManagerByDepartment(String department) {
        return employeeRepository.findAll().stream()
                .map(userAttributesMapper)
                .filter(employee -> employee.getDepartment().equals(department))
                .filter(employee -> employee.getRole().equals("Manager"))
                .findFirst()
                .orElseThrow(() -> new EmployeeNotFoundException("Manager not found"));

    }

    @Override
    public void updateEmployeeInfo(EmployeeInfoRequest employeeInfoRequest, MultipartFile picture) {

        Employee employee = (Employee) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (picture != null && !picture.isEmpty()) {
            try {
                employee.setPicture(picture.getBytes());
                employee.setPictureType(picture.getContentType());
            } catch (IOException e) {
                throw new FileUploadException("Failed to upload attachment");
            }
        }

        employee.setJobTitle(employeeInfoRequest.jobTitle());
        employee.setEmail(employeeInfoRequest.email());
        employee.setLastName(employeeInfoRequest.lastName());
        employee.setFirstName(employeeInfoRequest.firstName());
        employeeInfoRepository.save(employee);
    }

    @Override
    public ProfilePicture getProfilePicture(String username) {
        Optional<Employee> employee = employeeInfoRepository.findByUsername(username);
        log.info("Employee: {}",username);
        if (employee.isPresent()) {
            return employee.get().getPicture() != null
                    ? new ProfilePicture(employee.get().getPictureType(), employee.get().getPicture())
                    : null;
        }
        return null;
    }

}