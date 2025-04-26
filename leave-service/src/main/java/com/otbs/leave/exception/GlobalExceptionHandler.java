package com.otbs.leave.exception;

import com.otbs.leave.dto.MessageResponse;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({InvalidDateRangeException.class})
    public ResponseEntity<?> handleInvalidDateRange() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Leave date range is overlapping with existing leave"));
    }

    @ExceptionHandler({InvalidTimeRangeException.class})
    public ResponseEntity<?> handleInvalidTimeRange() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Invalid time range"));
    }

    @ExceptionHandler({MissingTimeForAuthorizationException.class})
    public ResponseEntity<?> handleMissingTimeForAuthorization() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Missing time for authorization"));
    }

    @ExceptionHandler({EmptyAttachmentException.class})
    public ResponseEntity<?> handleEmptyAttachment() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Empty attachment"));
    }

    @ExceptionHandler({LeaveNotFoundException.class})
    public ResponseEntity<?> handleLeaveNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Leave not found"));
    }

    @ExceptionHandler({LeaveBalanceNotFoundException.class})
    public ResponseEntity<?> handleLeaveBalanceNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Leave balance not found"));
    }

    @ExceptionHandler({InsufficientLeaveBalanceException.class})
    public ResponseEntity<?> handleInsufficientLeaveBalance() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Insufficient leave balance"));
    }

    @ExceptionHandler({FileUploadException.class})
    public ResponseEntity<?> handleFileUploadException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Failed to upload attachment"));
    }

    @ExceptionHandler({AttachmentNotFoundException.class})
    public ResponseEntity<?> handleAttachmentNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Attachment not found"));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(FeignException e) {
        return ResponseEntity.status(e.status()).body(new MessageResponse(e.getMessage()));
    }
}
