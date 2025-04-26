package com.otbs.training.exception;

import com.otbs.leave.dto.MessageResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TrainingException.class)
    public ResponseEntity<?> handleTrainingException(TrainingException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(InvitationException.class)
    public ResponseEntity<?> handleInvitationException(InvitationException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(FeignException e) {
        return ResponseEntity.status(e.status()).body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> response = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                response.put("error", violation.getMessage())
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
