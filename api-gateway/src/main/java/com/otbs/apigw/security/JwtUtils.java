package com.otbs.apigw.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
@Getter
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private int accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;


    public String generateAccessToken(String username, List<String> roles) {
        return buildToken(username, accessExpirationMs, "access", roles);
    }

    public String generateRefreshToken(String username, List<String> roles) {
        return buildToken(username, refreshExpirationMs, "refresh", roles);
    }

    private String buildToken(String username, long expiration, String tokenType, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("token_type", tokenType)
                .claim("roles", roles) // Add roles to JWT
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + expiration))
                .signWith((SecretKey) getSigningKey())
                .compact();
    }


    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "refresh".equals(claims.get("token_type"));
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public List<String> getRolesFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("roles", List.class);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) getSigningKey()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}