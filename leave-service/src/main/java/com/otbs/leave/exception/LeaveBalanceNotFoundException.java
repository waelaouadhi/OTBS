package com.otbs.leave.exception;

public class LeaveBalanceNotFoundException extends RuntimeException {
    public LeaveBalanceNotFoundException(String message) {
        super(message);
    }
}