package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AuthServiceConcurrencyTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID SESSION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String CURRENT_JTI = "current-refresh-jti";

    private UserRepository userRepository;
    private UserSessionService userSessionService;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionService = mock(UserSessionService.class);
        jwtTokenProvider = new JwtTokenProvider(
                new ObjectMapper(),
                "test-secret-key",
                "bread-diary"
        );
        authService = new AuthService(
                userRepository,
                userSessionService,
                jwtTokenProvider,
                mock(TossAuthClient.class),
                mock(TossUserInfoDecryptor.class)
        );
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void concurrentRefreshWithSameTokenAllowsOnlyOneSuccess() throws Exception {
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime refreshTokenExpiresAt = issuedAt.plusDays(30);
        String refreshToken = createRefreshToken(issuedAt, refreshTokenExpiresAt);
        UserSession userSession = activeSession(refreshToken, refreshTokenExpiresAt);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch firstRotationDoneLatch = new CountDownLatch(1);
        AtomicInteger findOrder = new AtomicInteger();

        when(userSessionService.findSessionForUpdate(SESSION_ID)).thenAnswer(invocation -> {
            if (findOrder.incrementAndGet() == 1) {
                return Optional.of(userSession);
            }

            assertTrue(firstRotationDoneLatch.await(2, TimeUnit.SECONDS));
            return Optional.of(userSession);
        });
        when(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(Optional.of(activeUser()));
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LocalDateTime.class).plusDays(1));
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LocalDateTime.class).plusDays(30));
        doAnswer(invocation -> {
            String refreshTokenHash = invocation.getArgument(1, String.class);
            String currentJti = invocation.getArgument(2, String.class);
            LocalDateTime rotatedAt = invocation.getArgument(3, LocalDateTime.class);

            userSession.rotateRefreshToken(
                    refreshTokenHash,
                    currentJti,
                    rotatedAt.plusDays(30)
            );
            firstRotationDoneLatch.countDown();
            return null;
        }).when(userSessionService).rotateRefreshToken(
                eq(userSession),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );

        Callable<RefreshAttemptResult> refreshAttempt = () -> {
            startLatch.await();
            return refresh(refreshToken);
        };

        Future<RefreshAttemptResult> firstFuture = executorService.submit(refreshAttempt);
        Future<RefreshAttemptResult> secondFuture = executorService.submit(refreshAttempt);

        startLatch.countDown();

        List<RefreshAttemptResult> results = List.of(
                firstFuture.get(3, TimeUnit.SECONDS),
                secondFuture.get(3, TimeUnit.SECONDS)
        );

        long successCount = results.stream()
                .filter(RefreshAttemptResult::success)
                .count();
        long conflictCount = results.stream()
                .filter(result -> HttpStatus.CONFLICT.equals(result.status()))
                .count();

        assertEquals(1L, successCount);
        assertEquals(1L, conflictCount);
        verify(userSessionService, times(1)).rotateRefreshToken(
                eq(userSession),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @Test
    void refreshAfterLogoutFailsBecauseSessionIsRevoked() {
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime refreshTokenExpiresAt = issuedAt.plusDays(30);
        String refreshToken = createRefreshToken(issuedAt, refreshTokenExpiresAt);
        UserSession userSession = activeSession(refreshToken, refreshTokenExpiresAt);

        when(userSessionService.findSessionForUpdate(SESSION_ID))
                .thenReturn(Optional.of(userSession), Optional.empty());

        authService.logout(new LogoutRequest(refreshToken));

        ResponseStatusException exception = assertRefreshThrows(refreshToken);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(userSessionService).revokeSession(eq(userSession), any(LocalDateTime.class));
    }

    @Test
    void refreshAfterWithdrawalFailsBecauseSessionWasDeleted() {
        LocalDateTime issuedAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime refreshTokenExpiresAt = issuedAt.plusDays(30);
        String refreshToken = createRefreshToken(issuedAt, refreshTokenExpiresAt);

        when(userSessionService.findSessionForUpdate(SESSION_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertRefreshThrows(refreshToken);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(userSessionService, never()).rotateRefreshToken(
                any(UserSession.class),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    private RefreshAttemptResult refresh(String refreshToken) {
        try {
            authService.refresh(new RefreshTokenRequest(refreshToken));
            return new RefreshAttemptResult(true, null);
        } catch (ResponseStatusException exception) {
            return new RefreshAttemptResult(false, (HttpStatus) exception.getStatusCode());
        }
    }

    private ResponseStatusException assertRefreshThrows(String refreshToken) {
        try {
            authService.refresh(new RefreshTokenRequest(refreshToken));
        } catch (ResponseStatusException exception) {
            return exception;
        }

        throw new AssertionError("refresh 요청이 실패해야 합니다.");
    }

    private String createRefreshToken(LocalDateTime issuedAt, LocalDateTime refreshTokenExpiresAt) {
        return jwtTokenProvider.createRefreshToken(
                USER_ID,
                SESSION_ID,
                CURRENT_JTI,
                issuedAt,
                refreshTokenExpiresAt
        );
    }

    private UserSession activeSession(String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        return UserSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .refreshTokenHash(hash(refreshToken))
                .currentJti(CURRENT_JTI)
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build();
    }

    private User activeUser() {
        return User.builder()
                .id(USER_ID)
                .tossUserKey("toss-user-key")
                .nickname("bread-lover")
                .email("bread@toss.im")
                .build();
    }

    private String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record RefreshAttemptResult(
            boolean success,
            HttpStatus status
    ) {
    }
}
