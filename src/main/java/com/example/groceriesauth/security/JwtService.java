package com.example.groceriesauth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final Key signKey;
    private final int expirationDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-days}") int expirationDays) {
        // Ensure we have at least a 256-bit (32 byte) key. If the provided secret is short
        // (e.g. a simple passphrase), derive a 256-bit key by hashing with SHA-256.
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to derive JWT signing key", e);
            }
        }
        this.signKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationDays = expirationDays;
    }

    public String issueToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token).getBody();
    }
}


