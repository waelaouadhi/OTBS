package com.otbs.auth.service;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.dto.EmployeeResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeClient employeeClient;

    public UserDetailsServiceImpl(EmployeeClient employeeClient) {
        this.employeeClient = employeeClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        EmployeeResponse user = employeeClient.getEmployeeByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return UserDetailsImpl.build(user);
    }
}