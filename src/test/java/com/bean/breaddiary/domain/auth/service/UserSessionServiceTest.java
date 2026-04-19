package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSessionServiceTest {

    private UserSessionRepository userSessionRepository;
    private UserSessionService userSessionService;

    @BeforeEach
    void setUp() {
        userSessionRepository = mock(UserSessionRepository.class);
        userSessionService = new UserSessionService(userSessionRepository);
    }

    @Test
    void createSessionUsesThirtyDayRefreshTtl() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 18, 10, 0);

        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSession actual = userSessionService.createSession(
                userId,
                "hashed-refresh-token",
                "refresh-jti",
                issuedAt
        );

        assertEquals(userId, actual.getUserId());
        assertEquals("hashed-refresh-token", actual.getRefreshTokenHash());
        assertEquals("refresh-jti", actual.getCurrentJti());
        assertEquals(issuedAt.plusDays(30), actual.getRefreshExpiresAt());
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void calculateAccessTokenExpiresAtUsesOneDayTtl() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 18, 10, 0);

        LocalDateTime actual = userSessionService.calculateAccessTokenExpiresAt(issuedAt);

        assertEquals(issuedAt.plusDays(1), actual);
    }

    @Test
    void findActiveSessionReturnsEmptyWhenRefreshIsExpired() {
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        LocalDateTime now = LocalDateTime.of(2026, 4, 18, 10, 0);
        UserSession expiredSession = createSession(
                sessionId,
                now.minusDays(1),
                null
        );

        when(userSessionRepository.findByIdAndRevokedAtIsNull(sessionId))
                .thenReturn(Optional.of(expiredSession));

        Optional<UserSession> actual = userSessionService.findActiveSession(sessionId, now);

        assertTrue(actual.isEmpty());
    }

    @Test
    void findActiveSessionForUpdateUsesLockedRepositoryMethod() {
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        LocalDateTime now = LocalDateTime.of(2026, 4, 18, 10, 0);
        UserSession activeSession = createSession(
                sessionId,
                now.plusDays(10),
                null
        );

        when(userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(sessionId))
                .thenReturn(Optional.of(activeSession));

        Optional<UserSession> actual = userSessionService.findActiveSessionForUpdate(sessionId, now);

        assertTrue(actual.isPresent());
        verify(userSessionRepository).findByIdAndRevokedAtIsNullForUpdate(sessionId);
    }

    @Test
    void findSessionForUpdateUsesLockedRepositoryMethod() {
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440012");
        UserSession activeSession = createSession(
                sessionId,
                LocalDateTime.of(2026, 4, 28, 10, 0),
                null
        );

        when(userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(sessionId))
                .thenReturn(Optional.of(activeSession));

        Optional<UserSession> actual = userSessionService.findSessionForUpdate(sessionId);

        assertTrue(actual.isPresent());
        verify(userSessionRepository).findByIdAndRevokedAtIsNullForUpdate(sessionId);
    }

    @Test
    void rotateRefreshTokenUpdatesHashJtiAndRefreshExpiry() {
        LocalDateTime rotatedAt = LocalDateTime.of(2026, 4, 18, 10, 0);
        UserSession userSession = createSession(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440003"),
                rotatedAt.plusDays(1),
                null
        );

        userSessionService.rotateRefreshToken(
                userSession,
                "next-hash",
                "next-jti",
                rotatedAt
        );

        assertEquals("next-hash", userSession.getRefreshTokenHash());
        assertEquals("next-jti", userSession.getCurrentJti());
        assertEquals(rotatedAt.plusDays(30), userSession.getRefreshExpiresAt());
        assertFalse(userSession.isRevoked());
    }

    @Test
    void revokeAllSessionsMarksAllFetchedSessionsAsRevoked() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        LocalDateTime revokedAt = LocalDateTime.of(2026, 4, 18, 10, 0);
        UserSession firstSession = createSession(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440005"),
                revokedAt.plusDays(10),
                null
        );
        UserSession secondSession = createSession(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440006"),
                revokedAt.plusDays(10),
                null
        );

        when(userSessionRepository.findAllByUserIdAndRevokedAtIsNull(userId))
                .thenReturn(List.of(firstSession, secondSession));

        userSessionService.revokeAllSessions(userId, revokedAt);

        assertEquals(revokedAt, firstSession.getRevokedAt());
        assertEquals(revokedAt, secondSession.getRevokedAt());
        verify(userSessionRepository).findAllByUserIdAndRevokedAtIsNull(userId);
    }

    private UserSession createSession(
            UUID sessionId,
            LocalDateTime refreshExpiresAt,
            LocalDateTime revokedAt
    ) {
        UserSession userSession = UserSession.builder()
                .id(sessionId)
                .userId(UUID.fromString("550e8400-e29b-41d4-a716-446655440099"))
                .refreshTokenHash("hashed-refresh-token")
                .currentJti("current-jti")
                .refreshExpiresAt(refreshExpiresAt)
                .revokedAt(revokedAt)
                .createdAt(LocalDateTime.of(2026, 4, 18, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 18, 9, 0))
                .build();

        assertNotNull(userSession);
        return userSession;
    }
}
