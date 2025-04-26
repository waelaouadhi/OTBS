package com.otbs.medVisit.controller;


import com.otbs.medVisit.dto.AppointmentRequest;
import com.otbs.medVisit.dto.AppointmentResponse;
import com.otbs.medVisit.dto.MessageResponse;
import com.otbs.medVisit.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<List<AppointmentResponse>> getAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable("id") Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByEmployeeId(@PathVariable("employeeId") String employeeId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatientId(employeeId));
    }

    @GetMapping("/medVisit/{medVisitId}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsByMedVisitId(@PathVariable("medVisitId") String medVisitId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByMedVisitId(medVisitId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> createAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest) {
        appointmentService.createAppointment(appointmentRequest);
        return ResponseEntity.ok(new MessageResponse("Appointment created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> updateAppointment(@PathVariable("id") Long id, @Valid @RequestBody AppointmentRequest appointmentRequest) {
        appointmentService.updateAppointment(appointmentRequest, id);
        return ResponseEntity.ok(new MessageResponse("Appointment updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> deleteAppointment(@PathVariable("id") Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(new MessageResponse("Appointment deleted successfully"));
    }
}
