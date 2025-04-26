package com.otbs.auth.exception;

import com.otbs.auth.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<MessageResponse> handleInvalidToken() {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid token");
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<MessageResponse> handleExpiredToken() {
        return buildResponse(HttpStatus.GONE, "Token expired");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<MessageResponse> handleUserNotFound() {
        return buildResponse(HttpStatus.NOT_FOUND, "User not found");
    }

    @ExceptionHandler(MailFailedException.class)
    public ResponseEntity<MessageResponse> handleMailFailed() {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email");
    }

    private ResponseEntity<MessageResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new MessageResponse(message));
    }
}