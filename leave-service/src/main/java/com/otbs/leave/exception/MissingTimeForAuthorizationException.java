package com.otbs.leave.exception;

public class MissingTimeForAuthorizationException extends RuntimeException {
    public MissingTimeForAuthorizationException(String message) {
        super(message);
    }
}
