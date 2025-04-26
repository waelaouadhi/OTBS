package com.otbs.auth.dto;

import com.otbs.feign.dto.EmployeeResponse;

public record JwtResponse(
        String accessToken,
        String refreshToken,
        long accessExpiration,
        long refreshExpiration,
        EmployeeResponse user
) {}