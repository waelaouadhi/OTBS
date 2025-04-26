package com.otbs.auth.service;

import com.otbs.auth.exception.UserNotFoundException;
import com.otbs.auth.repositories.PasswordResetTokenRepository;
import com.otbs.auth.exception.InvalidTokenException;
import com.otbs.auth.exception.TokenExpiredException;
import com.otbs.auth.model.PasswordResetToken;
import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.client.MailClient;
import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.feign.dto.MailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LdapTemplate ldapTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmployeeClient employeeClient;
    private final MailClient mailClient;

    public PasswordResetServiceImpl(PasswordResetTokenRepository passwordResetTokenRepository, LdapTemplate ldapTemplate, BCryptPasswordEncoder passwordEncoder, EmployeeClient employeeClient, MailClient mailClient) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.ldapTemplate = ldapTemplate;
        this.passwordEncoder = passwordEncoder;
        this.employeeClient = employeeClient;
        this.mailClient = mailClient;
    }

    @Override
    public void createPasswordResetTokenForUser(String email) {
        EmployeeResponse user = employeeClient.getEmployeeByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusSeconds(900);

        passwordResetTokenRepository.findByEmail(email).ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken passwordResetToken = new PasswordResetToken(user.id(), token, email, expiryDate);
        passwordResetTokenRepository.save(passwordResetToken);

        String resetPasswordLink = "http://localhost:4200/auth/reset-password?token=" + token;
        MailRequest mailRequest = new MailRequest(email, "Reset Password", "Password Reset Link: " + resetPasswordLink);
        mailClient.sendMail(mailRequest);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new TokenExpiredException("Token expired");
        }

        EmployeeResponse user = employeeClient.getEmployeeByEmail(resetToken.getEmail());
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        ModificationItem item = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd",
                passwordEncoder.encode(newPassword).getBytes(StandardCharsets.UTF_16LE)));
        ldapTemplate.modifyAttributes(user.id(), new ModificationItem[]{item});

        passwordResetTokenRepository.delete(resetToken);
    }
}