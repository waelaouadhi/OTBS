package com.otbs.employee.mapper;

import com.otbs.employee.model.Employee;
import com.otbs.employee.model.LdapUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class UserAttributesMapper implements Function<LdapUser, Employee> {

    @Override
    public Employee apply(LdapUser ldapUser) {
        Employee employee = new Employee();
        employee.setId(ldapUser.getDn().toString());
        employee.setUsername(ldapUser.getUsername());
        employee.setFirstName(ldapUser.getFirstName());
        employee.setLastName(ldapUser.getLastName());
        employee.setEmail(ldapUser.getEmail());
        employee.setDepartment(extractDepartment(ldapUser.getDn().toString()));
        employee.setRole(extractRole(ldapUser.getGroups().toString()));
        return employee;
    }

    private String extractDepartment(String dn) {
        for (String part : dn.split(",")) {
            if (part.startsWith("OU=")) {
                return part.split("=")[1];
            }
        }
        return "Unknown";
    }

    private String extractRole(String groups) {
        String[] groupParts = groups.split(",");
        log.info("groupParts: {}",groupParts);
        return groupParts.length > 0 ? groupParts[0].split("=")[1] : "Unknown";
    }
}