package com.otbs.medVisit.controller;


import com.otbs.medVisit.dto.MedicalVisitRequest;
import com.otbs.medVisit.dto.MedicalVisitResponse;
import com.otbs.medVisit.dto.MessageResponse;
import com.otbs.medVisit.service.MedicalVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/medical-visits")
@RequiredArgsConstructor
@Slf4j
@RestController
public class MedicalVisitController {

    private final MedicalVisitService medicalVisitService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Employee') or hasAuthority('Manager')")
    public ResponseEntity<List<MedicalVisitResponse>> getMedicalVisits() {
        return ResponseEntity.ok(medicalVisitService.getMedicalVisits());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR')")
    public ResponseEntity<MedicalVisitResponse> getMedicalVisit(@PathVariable("id") Long id) {
        return ResponseEntity.ok(medicalVisitService.getMedicalVisit(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR')")
    public ResponseEntity<MessageResponse> createMedicalVisit(@Valid @RequestBody MedicalVisitRequest medicalVisitRequest) {
        medicalVisitService.createMedicalVisit(medicalVisitRequest);
        return ResponseEntity.ok(new MessageResponse("Medical visit created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR')")
    public ResponseEntity<MessageResponse> updateMedicalVisit(@PathVariable("id") Long id, @Valid @RequestBody MedicalVisitRequest medicalVisitRequest) {
        medicalVisitService.updateMedicalVisit(medicalVisitRequest, id);
        return ResponseEntity.ok(new MessageResponse("Medical visit updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR')")
    public ResponseEntity<MessageResponse> deleteMedicalVisit(@PathVariable("id") Long id) {
        medicalVisitService.deleteMedicalVisit(id);
        return ResponseEntity.ok(new MessageResponse("Medical visit deleted successfully"));
    }
}
