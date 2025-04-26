package com.otbs.leave.exception;

public class AttachmentNotFoundException extends RuntimeException {
    public AttachmentNotFoundException(String message) {
        super(message);
    }
}
