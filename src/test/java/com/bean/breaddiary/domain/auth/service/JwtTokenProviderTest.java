package com.bean.breaddiary.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                new ObjectMapper(),
                "test-secret-key",
                "bread-diary"
        );
    }

    @Test
    void createAndParseAccessTokenContainsRequiredClaims() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 18, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 19, 10, 0);

        String token = jwtTokenProvider.createAccessToken(
                userId,
                sessionId,
                issuedAt,
                expiresAt
        );
        JwtTokenProvider.JwtTokenClaims claims = jwtTokenProvider.parseToken(token);

        assertEquals(userId, claims.userId());
        assertEquals(sessionId, claims.sessionId());
        assertEquals("access", claims.type());
        assertNull(claims.jti());
        assertEquals(issuedAt, claims.issuedAt());
        assertEquals(expiresAt, claims.expiresAt());
    }

    @Test
    void createAndParseRefreshTokenContainsJti() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 18, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        String token = jwtTokenProvider.createRefreshToken(
                userId,
                sessionId,
                "refresh-jti",
                issuedAt,
                expiresAt
        );
        JwtTokenProvider.JwtTokenClaims claims = jwtTokenProvider.parseToken(token);

        assertEquals(userId, claims.userId());
        assertEquals(sessionId, claims.sessionId());
        assertEquals("refresh", claims.type());
        assertEquals("refresh-jti", claims.jti());
        assertTrue(claims.isExpiredAt(LocalDateTime.of(2026, 5, 18, 10, 0)));
    }
}
