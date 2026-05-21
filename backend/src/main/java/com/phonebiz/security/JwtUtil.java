package com.phonebiz.security;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtil {

    @Value("${phonebiz.jwt.secret}")
    private String secret;

    @Value("${phonebiz.jwt.expiration}")
    private long expiration;

    /** Renew token if less than this many ms remaining (1 hour) */
    private static final long RENEW_THRESHOLD_MS = 3600_000L;

    public String generateToken(String username, String role, Long scopeOrgId, Long roleId, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(java.util.UUID.randomUUID().toString())
                .claim("role", role)
                .claim("scopeOrgId", scopeOrgId)
                .claim("roleId", roleId)
                .claim("permissions", permissions)
                .signWith(getSigningKey())
                .compact();
    }

    /** Legacy method for backward compatibility */
    public String generateToken(String username, String role, Long scopeOrgId) {
        return generateToken(username, role, scopeOrgId, null, List.of());
    }

    /** Legacy method */
    public String generateToken(String username) {
        return generateToken(username, null, null, null, List.of());
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public Long getScopeOrgIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("scopeOrgId", Long.class);
    }

    public Long getRoleIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roleId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        try {
            Object perms = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("permissions");
            if (perms instanceof List) {
                return (List<String>) perms;
            }
        } catch (Exception e) {
            log.warn("Failed to extract permissions from token: {}", e.getMessage());
        }
        return List.of();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * S12: Check if token should be renewed and return new token if so.
     * Returns null if renewal is not needed.
     */
    public String renewIfNeeded(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration2 = claims.getExpiration();
            long remaining = expiration2.getTime() - System.currentTimeMillis();

            if (remaining > 0 && remaining < RENEW_THRESHOLD_MS) {
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Long scopeOrgId = claims.get("scopeOrgId", Long.class);
                Long roleId = claims.get("roleId", Long.class);
                List<String> permissions = getPermissionsFromToken(token);

                String newToken = generateToken(username, role, scopeOrgId, roleId, permissions);
                log.info("Token renewed for user={}, remaining={}ms", username, remaining);
                return newToken;
            }
        } catch (Exception e) {
            log.debug("Token renewal check failed: {}", e.getMessage());
        }
        return null;
    }

    public long getExpiration() {
        return expiration;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
