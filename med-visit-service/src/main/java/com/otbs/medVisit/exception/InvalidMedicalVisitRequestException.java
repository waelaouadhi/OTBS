package com.otbs.medVisit.exception;

public class InvalidMedicalVisitRequestException extends RuntimeException {
    public InvalidMedicalVisitRequestException(String message) {
        super(message);
    }
}
