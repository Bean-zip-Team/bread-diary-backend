package com.bean.breaddiary.domain.auth.controller;

import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossWebhookRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.dto.response.LogoutResponse;
import com.bean.breaddiary.domain.auth.dto.response.TossWebhookResponse;
import com.bean.breaddiary.domain.auth.service.AuthService;
import com.bean.breaddiary.domain.user.service.UserWithdrawalService;
import com.bean.breaddiary.global.common.GlobalExceptionHandler;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private UserWithdrawalService userWithdrawalService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        userWithdrawalService = mock(UserWithdrawalService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, userWithdrawalService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void loginWithTossBindsSnakeCaseRequestAndSerializesCamelCaseResponse() throws Exception {
        AuthTokenResponse response = new AuthTokenResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "our-access-token",
                "our-refresh-token",
                "Bearer",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 5, 19, 10, 0),
                true
        );

        when(authService.loginWithToss(any(TossLoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/toss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorization_code": "auth-code",
                                  "referrer": "DEFAULT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.data.accessToken").value("our-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("our-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").value("2026-04-20T10:00:00"))
                .andExpect(jsonPath("$.data.refreshTokenExpiresAt").value("2026-05-19T10:00:00"))
                .andExpect(jsonPath("$.data.newUser").value(true))
                .andExpect(jsonPath("$.data.user_id").doesNotExist())
                .andExpect(jsonPath("$.data.access_token").doesNotExist());

        ArgumentCaptor<TossLoginRequest> requestCaptor = ArgumentCaptor.forClass(TossLoginRequest.class);
        verify(authService).loginWithToss(requestCaptor.capture());
        assertEquals("auth-code", requestCaptor.getValue().getAuthorizationCode());
        assertEquals("DEFAULT", requestCaptor.getValue().getReferrer());
    }

    @Test
    void loginWithTossBindsCamelCaseAuthorizationCodeFromTossClient() throws Exception {
        AuthTokenResponse response = new AuthTokenResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "our-access-token",
                "our-refresh-token",
                "Bearer",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 5, 19, 10, 0),
                true
        );

        when(authService.loginWithToss(any(TossLoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/toss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorizationCode": "auth-code-from-app-login",
                                  "referrer": "SANDBOX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("our-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("our-refresh-token"))
                .andExpect(jsonPath("$.data.access_token").doesNotExist());

        ArgumentCaptor<TossLoginRequest> requestCaptor = ArgumentCaptor.forClass(TossLoginRequest.class);
        verify(authService).loginWithToss(requestCaptor.capture());
        assertEquals("auth-code-from-app-login", requestCaptor.getValue().getAuthorizationCode());
        assertEquals("SANDBOX", requestCaptor.getValue().getReferrer());
    }

    @Test
    void refreshBindsCamelCaseRequestAndSerializesCamelCaseResponse() throws Exception {
        AuthTokenResponse response = new AuthTokenResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440010"),
                "new-access-token",
                "new-refresh-token",
                "Bearer",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 5, 19, 10, 0),
                false
        );

        when(authService.refresh(any(RefreshTokenRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.newUser").value(false))
                .andExpect(jsonPath("$.data.access_token").doesNotExist());

        ArgumentCaptor<RefreshTokenRequest> requestCaptor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(authService).refresh(requestCaptor.capture());
        assertEquals("refresh-token", requestCaptor.getValue().getRefreshToken());
    }

    @Test
    void logoutBindsCamelCaseRequestAndSerializesCamelCaseResponse() throws Exception {
        when(authService.logout(any(LogoutRequest.class)))
                .thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true))
                .andExpect(jsonPath("$.data.logged_out").doesNotExist());

        ArgumentCaptor<LogoutRequest> requestCaptor = ArgumentCaptor.forClass(LogoutRequest.class);
        verify(authService).logout(requestCaptor.capture());
        assertEquals("refresh-token", requestCaptor.getValue().getRefreshToken());
    }

    @Test
    void logoutAllowsEmptyBodyAndUsesAuthenticatedSession() throws Exception {
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        when(authService.logoutCurrentSession(sessionId))
                .thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/auth/logout")
                        .requestAttr(AuthRequestAttributes.SESSION_ID, sessionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true))
                .andExpect(jsonPath("$.data.logged_out").doesNotExist());

        verify(authService).logoutCurrentSession(sessionId);
    }

    @Test
    void logoutAllowsEmptyJsonObjectAndUsesAuthenticatedSession() throws Exception {
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440021");
        when(authService.logoutCurrentSession(sessionId))
                .thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/auth/logout")
                        .requestAttr(AuthRequestAttributes.SESSION_ID, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loggedOut").value(true))
                .andExpect(jsonPath("$.data.logged_out").doesNotExist());

        verify(authService).logoutCurrentSession(sessionId);
        verify(authService, never()).logout(any(LogoutRequest.class));
    }

    @Test
    void logoutRejectsFormUrlEncodedRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("refreshToken=refresh-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verify(authService, never()).logout(any(LogoutRequest.class));
        verify(authService, never()).logoutCurrentSession(any());
    }

    @Test
    void handleTossWebhookBindsCamelCaseRequestAndSerializesCamelCaseResponse() throws Exception {
        when(userWithdrawalService.handleTossWebhook(any(String.class), any(TossWebhookRequest.class)))
                .thenReturn(new TossWebhookResponse(true, "UNLINK"));

        mockMvc.perform(post("/auth/webhook/toss-unlink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic d2ViaG9vay1zZWNyZXQ=")
                        .content("""
                                {
                                  "userKey": "toss-user-key-12345678",
                                  "eventType": "UNLINK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processed").value(true))
                .andExpect(jsonPath("$.data.referrer").value("UNLINK"))
                .andExpect(jsonPath("$.data.eventType").doesNotExist());

        ArgumentCaptor<TossWebhookRequest> requestCaptor = ArgumentCaptor.forClass(TossWebhookRequest.class);
        ArgumentCaptor<String> secretCaptor = ArgumentCaptor.forClass(String.class);
        verify(userWithdrawalService).handleTossWebhook(secretCaptor.capture(), requestCaptor.capture());
        assertEquals("Basic d2ViaG9vay1zZWNyZXQ=", secretCaptor.getValue());
        assertEquals("toss-user-key-12345678", requestCaptor.getValue().getUserKey());
        assertEquals("UNLINK", requestCaptor.getValue().getReferrer());
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
