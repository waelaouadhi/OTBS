package com.otbs.training.service;

import com.otbs.training.dto.InvitationResponseDTO;
import com.otbs.training.exception.InvitationException;
import com.otbs.training.mapper.InvitationMapper;
import com.otbs.training.model.EStatus;
import com.otbs.training.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;

    @Override
    public void confirmInvitation(Long invitationId) {
        String employeeId = getCurrentUserId();
        invitationRepository.findByEmployeeIdAndTrainingId(employeeId, invitationId)
                .ifPresentOrElse(invitation -> {
                    invitation.setStatus(EStatus.CONFIRMED);
                    invitationRepository.save(invitation);
                }
                , () -> {
                    throw new InvitationException("Invitation not found for confirmation");
                });
    }

    @Override
    public void rejectInvitation(Long invitationId) {
        String employeeId = getCurrentUserId();
        invitationRepository.findByEmployeeIdAndTrainingId(employeeId, invitationId)
                .ifPresentOrElse(invitation -> {
                    invitation.setStatus(EStatus.REJECTED);
                    invitationRepository.save(invitation);
                }, () -> {
                    throw new InvitationException("Invitation not found for rejection");
                });
    }

    @Override
    public List<InvitationResponseDTO> getAllInvitations() {
        String employeeId = getCurrentUserId();

        return invitationRepository.findByEmployeeId(employeeId)
                .stream()
                .map(invitationMapper::toResponseDTO)
                .collect(Collectors.toList());

    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
