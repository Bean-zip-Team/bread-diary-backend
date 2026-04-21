package com.bean.breaddiary.domain.user.controller;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.service.JwtTokenProvider;
import com.bean.breaddiary.domain.auth.service.UserSessionService;
import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserStatsResponse;
import com.bean.breaddiary.domain.user.dto.response.UserWithdrawalResponse;
import com.bean.breaddiary.domain.user.service.UserService;
import com.bean.breaddiary.domain.user.service.UserWithdrawalService;
import com.bean.breaddiary.global.interceptor.AuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private static final UUID AUTHENTICATED_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID SESSION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final LocalDateTime PROFILE_CREATED_AT = LocalDateTime.of(2026, 4, 1, 0, 0);

    private UserService userService;
    private UserWithdrawalService userWithdrawalService;
    private UserSessionService userSessionService;
    private JwtTokenProvider jwtTokenProvider;
    private MockMvc mockMvc;
    private LocalDateTime tokenIssuedAt;
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userWithdrawalService = mock(UserWithdrawalService.class);
        userSessionService = mock(UserSessionService.class);
        jwtTokenProvider = new JwtTokenProvider(
                new ObjectMapper(),
                "test-secret-key",
                "bread-diary"
        );
        tokenIssuedAt = LocalDateTime.now().minusMinutes(1);
        accessTokenExpiresAt = tokenIssuedAt.plusDays(1);
        refreshTokenExpiresAt = tokenIssuedAt.plusDays(30);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService, userWithdrawalService))
                .addInterceptors(new AuthInterceptor(jwtTokenProvider, userSessionService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void getCurrentUserProfileReturnsCamelCaseResponse() throws Exception {
        UserMeResponse response = new UserMeResponse(
                AUTHENTICATED_USER_ID,
                "bread_lover",
                "bread@toss.im",
                "https://cdn.bread-diary.app/profiles/me.webp",
                "I always write down my bread diary.",
                new UserStatsResponse(42L, 18L, 4.2),
                PROFILE_CREATED_AT
        );

        when(userSessionService.findActiveSession(eq(SESSION_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSession()));
        when(userService.getCurrentUserProfile(AUTHENTICATED_USER_ID))
                .thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(AUTHENTICATED_USER_ID.toString()))
                .andExpect(jsonPath("$.data.nickname").value("bread_lover"))
                .andExpect(jsonPath("$.data.email").value("bread@toss.im"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.bread-diary.app/profiles/me.webp"))
                .andExpect(jsonPath("$.data.bio").value("I always write down my bread diary."))
                .andExpect(jsonPath("$.data.stats.totalRecords").value(42))
                .andExpect(jsonPath("$.data.stats.uniqueShops").value(18))
                .andExpect(jsonPath("$.data.stats.avgRating").value(4.2))
                .andExpect(jsonPath("$.data.createdAt").value("2026-04-01T00:00:00"))
                .andExpect(jsonPath("$.data.profile_image_url").doesNotExist())
                .andExpect(jsonPath("$.data.created_at").doesNotExist())
                .andExpect(jsonPath("$.data.stats.total_records").doesNotExist());

        verify(userService).getCurrentUserProfile(AUTHENTICATED_USER_ID);
    }

    @Test
    void withdrawCurrentUserReturnsSuccessResponse() throws Exception {
        when(userSessionService.findActiveSession(eq(SESSION_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSession()));
        when(userWithdrawalService.withdrawCurrentUser(AUTHENTICATED_USER_ID))
                .thenReturn(new UserWithdrawalResponse(true));

        mockMvc.perform(delete("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.withdrawn").value(true));

        verify(userWithdrawalService).withdrawCurrentUser(AUTHENTICATED_USER_ID);
    }

    @Test
    void getCurrentUserProfileWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, userWithdrawalService, userSessionService);
    }

    @Test
    void getCurrentUserProfileWithInvalidAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, userWithdrawalService, userSessionService);
    }

    @Test
    void getCurrentUserProfileReturnsNotFoundWhenUserIsDeleted() throws Exception {
        when(userSessionService.findActiveSession(eq(SESSION_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activeSession()));
        when(userService.getCurrentUserProfile(AUTHENTICATED_USER_ID))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 사용자를 찾을 수 없습니다."
                ));

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService).getCurrentUserProfile(AUTHENTICATED_USER_ID);
    }

    private UserSession activeSession() {
        return UserSession.builder()
                .id(SESSION_ID)
                .userId(AUTHENTICATED_USER_ID)
                .refreshTokenHash("hashed-refresh-token")
                .currentJti("current-jti")
                .refreshExpiresAt(refreshTokenExpiresAt)
                .build();
    }

    private String bearerAccessToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                AUTHENTICATED_USER_ID,
                SESSION_ID,
                tokenIssuedAt,
                accessTokenExpiresAt
        );
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
