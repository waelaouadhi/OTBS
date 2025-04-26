package com.otbs.feign.client;

import com.otbs.feign.dto.EmployeeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "employee-service", url = "http://localhost:8082")
public interface EmployeeClient {

    @GetMapping("api/v1/employee")
    EmployeeResponse getEmployeeByEmail(@RequestParam("email") String email);

    @GetMapping("api/v1/employee/{id}")
    EmployeeResponse getEmployeeByDn(@PathVariable("id") String id);

    @GetMapping("api/v1/employee/all")
    List<EmployeeResponse> getAllEmployees();

    @GetMapping("api/v1/employee/username")
    EmployeeResponse getEmployeeByUsername(@RequestParam("username") String username);

    @GetMapping("api/v1/employee/manager")
    EmployeeResponse getManagerByDepartment(@RequestParam("department") String department);
}