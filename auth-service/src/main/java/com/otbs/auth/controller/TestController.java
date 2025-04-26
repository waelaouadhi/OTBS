package com.otbs.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

        @GetMapping("/manager")
        @PreAuthorize("hasAuthority('Manager')")
        public ResponseEntity<?> testManager() {
                return ResponseEntity.ok("Manager Access Granted");
        }

        @GetMapping("/hr")
        @PreAuthorize("hasAuthority('HR')")
        public ResponseEntity<?> testHr() {
                return ResponseEntity.ok("HR Access Granted");
        }

        @GetMapping("/employee")
        @PreAuthorize("hasAuthority('Employee')")
        public ResponseEntity<?> testEmployee() {
                return ResponseEntity.ok("Employee Access Granted");
        }
}

