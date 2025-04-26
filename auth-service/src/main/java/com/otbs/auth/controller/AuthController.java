package com.otbs.auth.controller;

import com.otbs.auth.dto.JwtResponse;
import com.otbs.auth.dto.AuthRequest;
import com.otbs.auth.dto.RefreshRequest;
import com.otbs.auth.util.JwtUtils;
import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.dto.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmployeeClient employeeClient;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = jwtUtils.generateAccessToken(authRequest.username(), roles);

        EmployeeResponse response = employeeClient.getEmployeeByUsername(authRequest.username());
        EmployeeResponse user = response != null ? response : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                jwtUtils.generateRefreshToken(authRequest.username(), roles),
                jwtUtils.getAccessExpirationMs(),
                jwtUtils.getRefreshExpirationMs(),
                user
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();

        if (jwtUtils.validateRefreshToken(refreshToken)) {
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            EmployeeResponse response = employeeClient.getEmployeeByUsername(username);
            EmployeeResponse user = response != null ? response : null;
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<String> roles = List.of(user.role());
            String newAccessToken = jwtUtils.generateAccessToken(username, roles);
            String newRefreshToken = jwtUtils.generateRefreshToken(username, roles);

            return ResponseEntity.ok(new JwtResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtUtils.getAccessExpirationMs(),
                    jwtUtils.getRefreshExpirationMs(),
                    user
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}