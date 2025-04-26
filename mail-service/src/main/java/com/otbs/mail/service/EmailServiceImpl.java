package com.otbs.mail.service;

import com.otbs.mail.exception.MailFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendEmail(String subject, String to, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setTo(to);
            helper.setSubject(subject);

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("text", text);

            // Process HTML template
            String htmlContent = templateEngine.process("email-template", context);

            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailFailedException("Failed to send email: " + e.getMessage());
        }
    }
}