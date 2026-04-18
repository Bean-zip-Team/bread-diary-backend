package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.dto.response.LogoutResponse;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private UserSessionService userSessionService;
    private TossAuthClient tossAuthClient;
    private TossUserInfoDecryptor tossUserInfoDecryptor;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionService = mock(UserSessionService.class);
        tossAuthClient = mock(TossAuthClient.class);
        tossUserInfoDecryptor = mock(TossUserInfoDecryptor.class);
        jwtTokenProvider = new JwtTokenProvider(
                new ObjectMapper(),
                "test-secret-key",
                "bread-diary"
        );
        authService = new AuthService(
                userRepository,
                userSessionService,
                jwtTokenProvider,
                tossAuthClient,
                tossUserInfoDecryptor
        );
    }

    @Test
    void loginWithTossCreatesNewUserAndIssuesTokens() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        LocalDateTime accessTokenExpiresAt = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.of(2026, 5, 19, 10, 0);

        when(tossAuthClient.exchangeAuthorizationCode("auth-code", "DEFAULT"))
                .thenReturn(new TossAuthClient.TossGenerateTokenSuccess(
                        "toss-access-token",
                        "toss-refresh-token",
                        "bearer",
                        3600L,
                        "profile"
                ));
        when(tossAuthClient.getUserInfo("toss-access-token"))
                .thenReturn(new TossAuthClient.TossLoginMeSuccess(
                        TextNode.valueOf("toss-user-key-12345678"),
                        "encrypted-name",
                        "encrypted-email"
                ));
        when(tossUserInfoDecryptor.decryptNullable("encrypted-name"))
                .thenReturn("Bread Lover");
        when(tossUserInfoDecryptor.decryptNullable("encrypted-email"))
                .thenReturn("bread@toss.im");
        when(userRepository.findByTossUserKey("toss-user-key-12345678"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("bread@toss.im"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    return User.builder()
                            .id(userId)
                            .tossUserKey(savedUser.getTossUserKey())
                            .nickname(savedUser.getNickname())
                            .email(savedUser.getEmail())
                            .build();
                });
        when(userSessionService.createSession(
                eq(userId),
                eq("__PENDING_REFRESH_TOKEN__"),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash("__PENDING_REFRESH_TOKEN__")
                .currentJti("initial-jti")
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessTokenExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshTokenExpiresAt);

        AuthTokenResponse actual = authService.loginWithToss(
                new TossLoginRequest("auth-code", "DEFAULT")
        );

        assertEquals(userId, actual.getUserId());
        assertEquals("Bearer", actual.getTokenType());
        assertEquals(accessTokenExpiresAt, actual.getAccessTokenExpiresAt());
        assertEquals(refreshTokenExpiresAt, actual.getRefreshTokenExpiresAt());
        assertTrue(actual.isNewUser());
        assertNotNull(actual.getAccessToken());
        assertNotNull(actual.getRefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("toss-user-key-12345678", userCaptor.getValue().getTossUserKey());
        assertEquals("Bread Lover", userCaptor.getValue().getNickname());
        assertEquals("bread@toss.im", userCaptor.getValue().getEmail());

        ArgumentCaptor<String> refreshHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userSessionService).rotateRefreshToken(
                any(UserSession.class),
                refreshHashCaptor.capture(),
                anyString(),
                any(LocalDateTime.class)
        );
        assertNotEquals(actual.getRefreshToken(), refreshHashCaptor.getValue());
    }

    @Test
    void refreshRotatesRefreshTokenAndExtendsSlidingSession() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
        String currentJti = "current-refresh-jti";
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime accessTokenExpiresAt = issuedAt.plusDays(1);
        LocalDateTime refreshTokenExpiresAt = issuedAt.plusDays(30);
        String refreshToken = jwtTokenProvider.createRefreshToken(
                userId,
                sessionId,
                currentJti,
                issuedAt,
                refreshTokenExpiresAt
        );
        UserSession userSession = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(hash(refreshToken))
                .currentJti(currentJti)
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build();
        User user = User.builder()
                .id(userId)
                .tossUserKey("toss-user-key-refresh")
                .nickname("breadlover")
                .email("bread@toss.im")
                .build();

        when(userSessionService.findSessionForUpdate(sessionId))
                .thenReturn(Optional.of(userSession));
        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessTokenExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshTokenExpiresAt);

        AuthTokenResponse actual = authService.refresh(
                new RefreshTokenRequest(refreshToken)
        );

        JwtTokenProvider.JwtTokenClaims rotatedClaims = jwtTokenProvider.parseToken(actual.getRefreshToken());
        assertEquals(userId, actual.getUserId());
        assertEquals("Bearer", actual.getTokenType());
        assertEquals(accessTokenExpiresAt, actual.getAccessTokenExpiresAt());
        assertEquals(refreshTokenExpiresAt, actual.getRefreshTokenExpiresAt());
        assertFalse(actual.isNewUser());
        assertNotEquals(currentJti, rotatedClaims.jti());

        ArgumentCaptor<String> refreshHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userSessionService).rotateRefreshToken(
                eq(userSession),
                refreshHashCaptor.capture(),
                eq(rotatedClaims.jti()),
                any(LocalDateTime.class)
        );
        assertEquals(hash(actual.getRefreshToken()), refreshHashCaptor.getValue());
    }

    @Test
    void loginWithTossReactivatesDeletedUserFoundByTossUserKey() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        LocalDateTime accessTokenExpiresAt = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.of(2026, 5, 19, 10, 0);
        User deletedUser = User.builder()
                .id(userId)
                .tossUserKey("toss-user-key-87654321")
                .nickname("old-name")
                .email("old@toss.im")
                .deletedAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        prepareSuccessfulLogin(
                "toss-user-key-87654321",
                "Fresh Name",
                "fresh@toss.im",
                userId,
                sessionId,
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
        when(userRepository.findByTossUserKey("toss-user-key-87654321"))
                .thenReturn(Optional.of(deletedUser));

        AuthTokenResponse actual = authService.loginWithToss(
                new TossLoginRequest("auth-code", "DEFAULT")
        );

        assertFalse(actual.isNewUser());
        assertFalse(deletedUser.isDeleted());
        assertEquals("Fresh Name", deletedUser.getNickname());
        assertEquals("fresh@toss.im", deletedUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithTossLinksLegacyUserFoundByEmail() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
        LocalDateTime accessTokenExpiresAt = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.of(2026, 5, 19, 10, 0);
        User existingUser = User.builder()
                .id(userId)
                .nickname("legacy-user")
                .email("legacy@toss.im")
                .build();

        prepareSuccessfulLogin(
                "toss-user-key-99999999",
                "legacy-user",
                "legacy@toss.im",
                userId,
                sessionId,
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
        when(userRepository.findByTossUserKey("toss-user-key-99999999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("legacy@toss.im"))
                .thenReturn(Optional.of(existingUser));

        AuthTokenResponse actual = authService.loginWithToss(
                new TossLoginRequest("auth-code", "DEFAULT")
        );

        assertFalse(actual.isNewUser());
        assertEquals("toss-user-key-99999999", existingUser.getTossUserKey());
        assertEquals("legacy@toss.im", existingUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithTossUsesFallbackNicknameWhenDecryptionConfigIsMissing() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440006");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440007");
        LocalDateTime accessTokenExpiresAt = LocalDateTime.of(2026, 4, 20, 10, 0);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.of(2026, 5, 19, 10, 0);

        when(tossAuthClient.exchangeAuthorizationCode("auth-code", "DEFAULT"))
                .thenReturn(new TossAuthClient.TossGenerateTokenSuccess(
                        "toss-access-token",
                        "toss-refresh-token",
                        "bearer",
                        3600L,
                        "profile"
                ));
        when(tossAuthClient.getUserInfo("toss-access-token"))
                .thenReturn(new TossAuthClient.TossLoginMeSuccess(
                        TextNode.valueOf("toss-user-key-fallback"),
                        "encrypted-name",
                        null
                ));
        when(tossUserInfoDecryptor.decryptNullable("encrypted-name"))
                .thenReturn(null);
        when(userRepository.findByTossUserKey("toss-user-key-fallback"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    return User.builder()
                            .id(userId)
                            .tossUserKey(savedUser.getTossUserKey())
                            .nickname(savedUser.getNickname())
                            .email(savedUser.getEmail())
                            .build();
                });
        when(userSessionService.createSession(
                eq(userId),
                eq("__PENDING_REFRESH_TOKEN__"),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash("__PENDING_REFRESH_TOKEN__")
                .currentJti("initial-jti")
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessTokenExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshTokenExpiresAt);

        AuthTokenResponse actual = authService.loginWithToss(
                new TossLoginRequest("auth-code", "DEFAULT")
        );

        assertTrue(actual.isNewUser());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getNickname().startsWith("toss-"));
        assertNull(userCaptor.getValue().getEmail());
    }

    @Test
    void refreshRejectsRotatedRefreshTokenReuse() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440021");
        String oldJti = "old-refresh-jti";
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime expiresAt = issuedAt.plusDays(30);
        String refreshToken = jwtTokenProvider.createRefreshToken(
                userId,
                sessionId,
                oldJti,
                issuedAt,
                expiresAt
        );
        UserSession rotatedSession = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(hash("new-refresh-token"))
                .currentJti("new-refresh-jti")
                .refreshExpiresAt(expiresAt)
                .build();

        when(userSessionService.findSessionForUpdate(sessionId))
                .thenReturn(Optional.of(rotatedSession));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(new RefreshTokenRequest(refreshToken))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userSessionService, never()).rotateRefreshToken(
                any(UserSession.class),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @Test
    void logoutRevokesCurrentSession() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440031");
        String currentJti = "logout-jti";
        LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime expiresAt = issuedAt.plusDays(30);
        String refreshToken = jwtTokenProvider.createRefreshToken(
                userId,
                sessionId,
                currentJti,
                issuedAt,
                expiresAt
        );
        UserSession userSession = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash(hash(refreshToken))
                .currentJti(currentJti)
                .refreshExpiresAt(expiresAt)
                .build();

        when(userSessionService.findSessionForUpdate(sessionId))
                .thenReturn(Optional.of(userSession));

        LogoutResponse actual = authService.logout(
                new LogoutRequest(refreshToken)
        );

        assertTrue(actual.isLoggedOut());
        verify(userSessionService).revokeSession(eq(userSession), any(LocalDateTime.class));
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

    private void prepareSuccessfulLogin(
            String tossUserKey,
            String decryptedName,
            String decryptedEmail,
            UUID userId,
            UUID sessionId,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt
    ) {
        when(tossAuthClient.exchangeAuthorizationCode("auth-code", "DEFAULT"))
                .thenReturn(new TossAuthClient.TossGenerateTokenSuccess(
                        "toss-access-token",
                        "toss-refresh-token",
                        "bearer",
                        3600L,
                        "profile"
                ));
        when(tossAuthClient.getUserInfo("toss-access-token"))
                .thenReturn(new TossAuthClient.TossLoginMeSuccess(
                        TextNode.valueOf(tossUserKey),
                        "encrypted-name",
                        "encrypted-email"
                ));
        when(tossUserInfoDecryptor.decryptNullable("encrypted-name"))
                .thenReturn(decryptedName);
        when(tossUserInfoDecryptor.decryptNullable("encrypted-email"))
                .thenReturn(decryptedEmail);
        when(userSessionService.createSession(
                eq(userId),
                eq("__PENDING_REFRESH_TOKEN__"),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash("__PENDING_REFRESH_TOKEN__")
                .currentJti("initial-jti")
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessTokenExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshTokenExpiresAt);
    }
}
