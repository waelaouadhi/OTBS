package com.otbs.feign.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record EmployeeResponse(
        @JsonDeserialize(as = String.class) String id,
        String username,
        String firstName,
        String lastName,
        String email,
        String department,
        String role,
        String jobTitle
) {

}
