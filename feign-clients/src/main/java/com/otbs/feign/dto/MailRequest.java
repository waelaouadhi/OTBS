package com.otbs.feign.dto;

public record MailRequest(String to, String subject, String body) {
}
