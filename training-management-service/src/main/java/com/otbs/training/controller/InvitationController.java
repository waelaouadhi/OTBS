package com.otbs.training.controller;

import com.otbs.leave.dto.MessageResponse;
import com.otbs.training.dto.InvitationResponseDTO;
import com.otbs.training.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    @PutMapping("/confirm/{id}")
    @PreAuthorize("hasAuthority('Employee')")
    public ResponseEntity<MessageResponse> confirmInvitation(@PathVariable("id") Long id) {
        invitationService.confirmInvitation(id);
        return ResponseEntity.ok(new MessageResponse("Invitation confirmed successfully"));
    }

    @PutMapping("/reject/{id}")
    @PreAuthorize("hasAuthority('Employee')")
    public ResponseEntity<MessageResponse> rejectInvitation(@PathVariable("id") Long id) {
        invitationService.rejectInvitation(id);
        return ResponseEntity.ok(new MessageResponse("Invitation rejected successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('Employee')")
    public ResponseEntity<List<InvitationResponseDTO>> getAllInvitations() {
        return ResponseEntity.ok(invitationService.getAllInvitations());
    }
}
