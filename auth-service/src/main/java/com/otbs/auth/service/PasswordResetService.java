package com.otbs.auth.service;

public interface PasswordResetService {
    void createPasswordResetTokenForUser(String email);
    void resetPassword(String token, String password);
}
