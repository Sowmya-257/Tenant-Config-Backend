package com.moveinsync.config.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Generates and validates cryptographic JWT tokens with embedded tenant and role claims.
 */
@Component
public class JwtTokenProvider {

    // 256-bit secret key for HMAC-SHA256 signature
    private static final String SECRET_STRING = "MoveInSyncSecretKeyForHierarchicalConfigAuthentication2026Secure";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    // Generates a signed JWT token encapsulating username, tenantId, and user role.
    public String generateToken(String username, String tenantId, UserRole role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    // Validates a JWT token string and extracts its claims payload.
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extracts the tenant ID embedded in the JWT token.
    public String getTenantId(String token) {
        Claims claims = parseToken(token);
        return claims.get("tenantId", String.class);
    }

    // Extracts the user role embedded in the JWT token.
    public UserRole getRole(String token) {
        Claims claims = parseToken(token);
        String roleStr = claims.get("role", String.class);
        return UserRole.valueOf(roleStr);
    }
}
