package com.airline.booking.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the HS256 JWTs returned by {@code POST /api/v1/auth/login}. The token
 * carries the authenticated username as subject and a single {@code role} claim (e.g. "ADMIN" or
 * "CLIENT", without the Spring Security "ROLE_" prefix) - that claim is the sole source of
 * authorization for every subsequent request, read by {@link JwtAuthenticationFilter}.
 */
@Component
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey, ALGORITHM)
                .compact();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    /** Empty if the token is missing, malformed, expired, or signed with a different key. */
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new ParsedToken(claims.getSubject(), claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record ParsedToken(String username, String role) {
    }
}
