package com.otbs.employee.exception;

import com.otbs.employee.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<MessageResponse> handleEmployeeNotFound() {
        return new ResponseEntity<>(new MessageResponse("Employee not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<MessageResponse> handleFileUploadException() {
        return new ResponseEntity<>(new MessageResponse("File upload failed"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}