package com.otbs.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private String dn;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public PasswordResetToken(String dn, String token, String email, Instant expiryDate) {
        this.dn = dn;
        this.token = token;
        this.email = email;
        this.expiryDate = expiryDate;
    }
}