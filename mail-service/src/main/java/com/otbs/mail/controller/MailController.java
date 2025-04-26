package com.otbs.mail.controller;

import com.otbs.mail.dto.MailRequest;
import com.otbs.mail.dto.MailResponse;
import com.otbs.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mail")
public class MailController {

    private final EmailService mailService;

    @PostMapping("/send")
    public MailResponse sendMail(@RequestBody MailRequest mailRequest) {
        log.info("Sending email to: {}", mailRequest.to());
        mailService.sendEmail(mailRequest.subject(), mailRequest.to(),mailRequest.body());
        return new MailResponse("Email sent successfully");
    }
}
