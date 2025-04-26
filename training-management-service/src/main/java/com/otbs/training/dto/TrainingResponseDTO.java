package com.otbs.training.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingResponseDTO(
        Long id,
        String title,
        String description,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        String createdBy,
        List<InvitationResponseDTO> invitations,
        LocalDateTime createdAt
) {
}
