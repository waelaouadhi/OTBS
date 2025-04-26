package com.otbs.mail.exception;

public class MailFailedException extends RuntimeException {
    public MailFailedException(String message) {
        super(message);
    }
}
