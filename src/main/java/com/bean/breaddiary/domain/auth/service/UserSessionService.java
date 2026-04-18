package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSessionService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final UserSessionRepository userSessionRepository;

    @Transactional
    public UserSession createSession(
            UUID userId,
            String refreshTokenHash,
            String currentJti,
            LocalDateTime issuedAt
    ) {
        UserSession userSession = UserSession.builder()
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .currentJti(currentJti)
                .refreshExpiresAt(calculateRefreshTokenExpiresAt(issuedAt))
                .build();

        return userSessionRepository.save(userSession);
    }

    public Optional<UserSession> findActiveSession(UUID sessionId, LocalDateTime now) {
        LocalDateTime targetTime = resolveTime(now);

        return userSessionRepository.findByIdAndRevokedAtIsNull(sessionId)
                .filter(userSession -> !userSession.isRefreshExpiredAt(targetTime));
    }

    public Optional<UserSession> findSession(UUID sessionId) {
        return userSessionRepository.findByIdAndRevokedAtIsNull(sessionId);
    }

    public Optional<UserSession> findSessionForUpdate(UUID sessionId) {
        return userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(sessionId);
    }

    public Optional<UserSession> findActiveSessionForUpdate(UUID sessionId, LocalDateTime now) {
        LocalDateTime targetTime = resolveTime(now);

        return userSessionRepository.findByIdAndRevokedAtIsNullForUpdate(sessionId)
                .filter(userSession -> !userSession.isRefreshExpiredAt(targetTime));
    }

    @Transactional
    public void rotateRefreshToken(
            UserSession userSession,
            String refreshTokenHash,
            String currentJti,
            LocalDateTime rotatedAt
    ) {
        userSession.rotateRefreshToken(
                refreshTokenHash,
                currentJti,
                calculateRefreshTokenExpiresAt(rotatedAt)
        );
    }

    @Transactional
    public void revokeSession(UserSession userSession, LocalDateTime revokedAt) {
        userSession.revoke(resolveTime(revokedAt));
    }

    @Transactional
    public void revokeAllSessions(UUID userId, LocalDateTime revokedAt) {
        LocalDateTime targetTime = resolveTime(revokedAt);

        userSessionRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(userSession -> userSession.revoke(targetTime));
    }

    public LocalDateTime calculateAccessTokenExpiresAt(LocalDateTime issuedAt) {
        return resolveTime(issuedAt).plus(ACCESS_TOKEN_TTL);
    }

    public LocalDateTime calculateRefreshTokenExpiresAt(LocalDateTime issuedAt) {
        return resolveTime(issuedAt).plus(REFRESH_TOKEN_TTL);
    }

    private LocalDateTime resolveTime(LocalDateTime dateTime) {
        return dateTime == null ? LocalDateTime.now() : dateTime;
    }
}
