package com.otbs.medVisit.exception;

import com.otbs.medVisit.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(AppointmentException.class)
    public ResponseEntity<MessageResponse> handleAppointmentNotFoundException(AppointmentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(TimeSlotNotAvailableException.class)
    public ResponseEntity<MessageResponse> handleTimeSlotNotAvailableException(TimeSlotNotAvailableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(MedicalVisitException.class)
    public ResponseEntity<MessageResponse> handleMedicalVisitNotFoundException(MedicalVisitException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidMedicalVisitRequestException.class)
    public ResponseEntity<MessageResponse> handleInvalidMedicalVisitRequestException(InvalidMedicalVisitRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

}
