package com.otbs.employee.service;

import com.otbs.employee.dto.EmployeeInfoRequest;
import com.otbs.employee.dto.ProfilePicture;
import com.otbs.employee.model.Employee;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.Name;
import java.util.List;

public interface EmployeeService {
    Employee getEmployeeByDn(Name dn);
    Employee getEmployeeByEmail(String email);
    List<Employee> getAllEmployees();
    Employee getEmployeeByUsername(String username);
    Employee getManagerByDepartment(String department);
    void updateEmployeeInfo(EmployeeInfoRequest employeeInfoRequest, MultipartFile picture);
    ProfilePicture getProfilePicture(String id);
}
