package com.otbs.leave.dto;

import com.otbs.leave.exception.*;
import com.otbs.leave.model.ELeaveType;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

public record LeaveRequest(
        @NotBlank(message = "Leave type is required") ELeaveType leaveType,
        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future") LocalDate startDate,
        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date must be today or in the future") LocalDate endDate,
        LocalTime startHOURLY,
        LocalTime endHOURLY
) {
    public LeaveRequest {
        validateDates(startDate, endDate);
        validateTimes(startHOURLY, endHOURLY);
        validateAuthorizationLeave(leaveType, startHOURLY, endHOURLY);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("Start date must be before or equal to end date");
        }
    }

    private boolean isSpecialLeaveType(ELeaveType leaveType) {
        return leaveType == ELeaveType.DÉCÈS || leaveType == ELeaveType.MATERNITÉ ||
               leaveType == ELeaveType.PATERNITÉ || leaveType == ELeaveType.MALADIE;
    }

    private void validateTimes(LocalTime startHOURLY, LocalTime endHOURLY) {
        if (startHOURLY != null && endHOURLY != null && startHOURLY.isAfter(endHOURLY)) {
            throw new InvalidTimeRangeException("Start time must be before or equal to end time");
        }
    }

    private void validateAuthorizationLeave(ELeaveType leaveType, LocalTime startHOURLY, LocalTime endHOURLY) {
        if (leaveType == ELeaveType.AUTORISATION && (startHOURLY == null || endHOURLY == null)) {
            throw new MissingTimeForAuthorizationException("Start and end time are required for authorization leave type");
        }
    }
}