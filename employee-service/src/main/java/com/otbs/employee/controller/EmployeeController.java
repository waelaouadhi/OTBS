package com.otbs.employee.controller;

import com.otbs.employee.dto.EmployeeInfoRequest;
import com.otbs.employee.dto.MessageResponse;
import com.otbs.employee.dto.ProfilePicture;
import com.otbs.employee.model.Employee;
import com.otbs.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Employee getEmployeeByEmail(@RequestParam("email") String email) {
        return employeeService.getEmployeeByEmail(email);
    }

    @GetMapping("/{id}")
    public Employee getEmployeeByDn(@PathVariable("id") String id) {
        return employeeService.getEmployeeByDn(LdapUtils.newLdapName(id));
    }

    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/username")
    public Employee getEmployeeByUsername(@RequestParam("username") String username) {
        return employeeService.getEmployeeByUsername(username);
    }

    //get manager by department
    @GetMapping("/manager")
    public Employee getManagerByDepartment(@RequestParam("department") String department) {
        return employeeService.getManagerByDepartment(department);
    }


    @PutMapping("/update")
    @PreAuthorize("hasAuthority('Manager') or hasAuthority('HR') or hasAuthority('Employee')")
    public ResponseEntity<MessageResponse> updateEmployeeInfo(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestParam("jobTitle") String department
    ) {
        EmployeeInfoRequest employeeInfoRequest = new EmployeeInfoRequest(firstName, lastName, email, department);
        employeeService.updateEmployeeInfo(employeeInfoRequest, picture);

        return ResponseEntity.ok(new MessageResponse("Employee updated successfully"));
    }

    @GetMapping("/profilePicture")
    public ResponseEntity<ProfilePicture> getProfilePicture(@RequestParam("username") String username) {
        return ResponseEntity.ok(employeeService.getProfilePicture(username));
    }
}