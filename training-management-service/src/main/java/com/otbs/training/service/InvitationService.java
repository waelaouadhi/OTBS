package com.otbs.training.service;

import com.otbs.training.dto.InvitationResponseDTO;

import java.util.List;

public interface InvitationService {
    void confirmInvitation(Long invitationId);
    void rejectInvitation(Long invitationId);
    List<InvitationResponseDTO> getAllInvitations();
}
