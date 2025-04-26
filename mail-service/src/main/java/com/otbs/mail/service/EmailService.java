package com.otbs.mail.service;

public interface EmailService {
    void sendEmail(String subject, String to, String text);
}
