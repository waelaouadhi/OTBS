package com.otbs.mail.dto;

public record MailRequest(String to, String subject, String body) {
}
