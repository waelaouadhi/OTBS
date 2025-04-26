package com.otbs.medVisit.dto;

import com.otbs.medVisit.exception.InvalidMedicalVisitRequestException;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record MedicalVisitRequest(
        @NotBlank(message = "Doctor name is required")
        @Size(min = 2, max = 100, message = "Doctor name must be between 2 and 100 characters")
        String doctorName,

        @NotNull(message = "Visit date is required")
        @FutureOrPresent(message = "Visit date must be today or in the future")
        LocalDate visitDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {
    public MedicalVisitRequest {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new InvalidMedicalVisitRequestException("End time must be after start time");
        }
    }
}
