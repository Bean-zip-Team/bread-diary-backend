package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private JwtTokenProvider jwtTokenProvider;
    private TossAuthClient tossAuthClient;
    private TossUserInfoDecryptor tossUserInfoDecryptor;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionService = mock(UserSessionService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        tossAuthClient = mock(TossAuthClient.class);
        tossUserInfoDecryptor = mock(TossUserInfoDecryptor.class);
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
        LocalDateTime accessExpiresAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime refreshExpiresAt = LocalDateTime.of(2026, 5, 18, 10, 0);
        TossLoginRequest request = new TossLoginRequest("auth-code", "DEFAULT");

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
                    User user = invocation.getArgument(0);
                    return User.builder()
                            .id(userId)
                            .tossUserKey(user.getTossUserKey())
                            .nickname(user.getNickname())
                            .email(user.getEmail())
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
                .refreshExpiresAt(refreshExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshExpiresAt);
        when(jwtTokenProvider.createAccessToken(
                eq(userId),
                eq(sessionId),
                any(LocalDateTime.class),
                eq(accessExpiresAt)
        )).thenReturn("our-access-token");
        when(jwtTokenProvider.createRefreshToken(
                eq(userId),
                eq(sessionId),
                anyString(),
                any(LocalDateTime.class),
                eq(refreshExpiresAt)
        )).thenReturn("our-refresh-token");

        AuthTokenResponse actual = authService.loginWithToss(request);

        assertEquals(userId, actual.getUserId());
        assertEquals("our-access-token", actual.getAccessToken());
        assertEquals("our-refresh-token", actual.getRefreshToken());
        assertEquals("Bearer", actual.getTokenType());
        assertEquals(accessExpiresAt, actual.getAccessTokenExpiresAt());
        assertEquals(refreshExpiresAt, actual.getRefreshTokenExpiresAt());
        assertTrue(actual.isNewUser());

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
        assertNotEquals("our-refresh-token", refreshHashCaptor.getValue());
    }

    @Test
    void loginWithTossReactivatesDeletedUserFoundByTossUserKey() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        LocalDateTime accessExpiresAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime refreshExpiresAt = LocalDateTime.of(2026, 5, 18, 10, 0);
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
                accessExpiresAt,
                refreshExpiresAt
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
        LocalDateTime accessExpiresAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime refreshExpiresAt = LocalDateTime.of(2026, 5, 18, 10, 0);
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
                accessExpiresAt,
                refreshExpiresAt
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
        LocalDateTime accessExpiresAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        LocalDateTime refreshExpiresAt = LocalDateTime.of(2026, 5, 18, 10, 0);

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
                    User user = invocation.getArgument(0);
                    return User.builder()
                            .id(userId)
                            .tossUserKey(user.getTossUserKey())
                            .nickname(user.getNickname())
                            .email(user.getEmail())
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
                .refreshExpiresAt(refreshExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshExpiresAt);
        when(jwtTokenProvider.createAccessToken(
                eq(userId),
                eq(sessionId),
                any(LocalDateTime.class),
                eq(accessExpiresAt)
        )).thenReturn("our-access-token");
        when(jwtTokenProvider.createRefreshToken(
                eq(userId),
                eq(sessionId),
                anyString(),
                any(LocalDateTime.class),
                eq(refreshExpiresAt)
        )).thenReturn("our-refresh-token");

        AuthTokenResponse actual = authService.loginWithToss(
                new TossLoginRequest("auth-code", "DEFAULT")
        );

        assertTrue(actual.isNewUser());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getNickname().startsWith("toss-"));
        assertNull(userCaptor.getValue().getEmail());
    }

    private void prepareSuccessfulLogin(
            String tossUserKey,
            String decryptedName,
            String decryptedEmail,
            UUID userId,
            UUID sessionId,
            LocalDateTime accessExpiresAt,
            LocalDateTime refreshExpiresAt
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
                .refreshExpiresAt(refreshExpiresAt)
                .build());
        when(userSessionService.calculateAccessTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(accessExpiresAt);
        when(userSessionService.calculateRefreshTokenExpiresAt(any(LocalDateTime.class)))
                .thenReturn(refreshExpiresAt);
        when(jwtTokenProvider.createAccessToken(
                eq(userId),
                eq(sessionId),
                any(LocalDateTime.class),
                eq(accessExpiresAt)
        )).thenReturn("our-access-token");
        when(jwtTokenProvider.createRefreshToken(
                eq(userId),
                eq(sessionId),
                anyString(),
                any(LocalDateTime.class),
                eq(refreshExpiresAt)
        )).thenReturn("our-refresh-token");
    }
}
