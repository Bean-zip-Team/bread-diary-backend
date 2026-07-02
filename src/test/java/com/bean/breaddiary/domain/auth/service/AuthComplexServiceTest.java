package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.onboarding.service.OnboardingComplexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthComplexServiceTest {

    private AuthService authService;
    private OnboardingComplexService onboardingComplexService;
    private AuthComplexService authComplexService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        onboardingComplexService = mock(OnboardingComplexService.class);
        authComplexService = new AuthComplexService(authService, onboardingComplexService);
    }

    @Test
    void loginWithTossSkipsOnboardingSyncWhenSelectionIsMissing() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TossLoginRequest request = new TossLoginRequest("auth-code", "DEFAULT", null);
        AuthTokenResponse response = new AuthTokenResponse(
                userId,
                "access",
                "refresh",
                "Bearer",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 5, 19, 10, 0),
                true
        );

        when(authService.loginWithToss(request)).thenReturn(response);

        authComplexService.loginWithToss(request);

        verify(authService).loginWithToss(request);
        verify(onboardingComplexService).synchronizeSelectedBreads(userId, null);
    }

    @Test
    void loginWithTossDelegatesSelectedBreadIdsToOnboardingComplexService() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID firstBreadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID secondBreadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        TossLoginRequest request = new TossLoginRequest(
                "auth-code",
                "DEFAULT",
                List.of(firstBreadId, secondBreadId, firstBreadId)
        );
        AuthTokenResponse response = new AuthTokenResponse(
                userId,
                "access",
                "refresh",
                "Bearer",
                LocalDateTime.of(2026, 4, 20, 10, 0),
                LocalDateTime.of(2026, 5, 19, 10, 0),
                true
        );

        when(authService.loginWithToss(request)).thenReturn(response);

        authComplexService.loginWithToss(request);

        verify(onboardingComplexService).synchronizeSelectedBreads(userId, request.getSelectedBreadIds());
    }
}
