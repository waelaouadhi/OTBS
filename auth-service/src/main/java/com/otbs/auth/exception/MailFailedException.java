package com.otbs.auth.exception;

public class MailFailedException extends RuntimeException {
    public MailFailedException(String message) {
        super(message);
    }
}
