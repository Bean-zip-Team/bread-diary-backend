package com.bean.breaddiary.domain.auth.controller;

import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.dto.response.LogoutResponse;
import com.bean.breaddiary.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void loginWithTossBindsSnakeCaseRequestAndSerializesSnakeCaseResponse() throws Exception {
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
                .andExpect(jsonPath("$.data.user_id").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.data.access_token").value("our-access-token"))
                .andExpect(jsonPath("$.data.refresh_token").value("our-refresh-token"))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.access_token_expires_at").value("2026-04-20T10:00:00"))
                .andExpect(jsonPath("$.data.refresh_token_expires_at").value("2026-05-19T10:00:00"))
                .andExpect(jsonPath("$.data.new_user").value(true));

        ArgumentCaptor<TossLoginRequest> requestCaptor = ArgumentCaptor.forClass(TossLoginRequest.class);
        verify(authService).loginWithToss(requestCaptor.capture());
        assertEquals("auth-code", requestCaptor.getValue().getAuthorizationCode());
        assertEquals("DEFAULT", requestCaptor.getValue().getReferrer());
    }

    @Test
    void refreshBindsSnakeCaseRequestAndSerializesSnakeCaseResponse() throws Exception {
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
                                  "refresh_token": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.data.refresh_token").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.new_user").value(false));

        ArgumentCaptor<RefreshTokenRequest> requestCaptor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(authService).refresh(requestCaptor.capture());
        assertEquals("refresh-token", requestCaptor.getValue().getRefreshToken());
    }

    @Test
    void logoutBindsSnakeCaseRequestAndSerializesSnakeCaseResponse() throws Exception {
        when(authService.logout(any(LogoutRequest.class)))
                .thenReturn(new LogoutResponse(true));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logged_out").value(true))
                .andExpect(jsonPath("$.data.loggedOut").doesNotExist());

        ArgumentCaptor<LogoutRequest> requestCaptor = ArgumentCaptor.forClass(LogoutRequest.class);
        verify(authService).logout(requestCaptor.capture());
        assertEquals("refresh-token", requestCaptor.getValue().getRefreshToken());
    }

    private ObjectMapper snakeCaseObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
