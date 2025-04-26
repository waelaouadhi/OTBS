package com.otbs.medVisit.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull(message = "Medical visit ID is required")
        @Positive(message = "Medical visit ID must be a positive number")
        Long medicalVisitId,

        @NotNull(message = "Time slot is required")
        @Future(message = "Time slot must be in the future")
        LocalDateTime timeSlot
) {}