package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.bean.breaddiary.domain.breadtype.BreadTypeTestFixture;
import com.bean.breaddiary.domain.onboarding.service.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthComplexServiceTest {

    private AuthService authService;
    private BreadService breadService;
    private OnboardingService onboardingService;
    private AuthComplexService authComplexService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        breadService = mock(BreadService.class);
        onboardingService = mock(OnboardingService.class);
        authComplexService = new AuthComplexService(authService, breadService, onboardingService);
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
        verifyNoOnboardingInteractions();
    }

    @Test
    void loginWithTossReplacesOnboardingSelectionsAfterSuccessfulLogin() {
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
        Bread firstBread = Bread.builder().id(firstBreadId).breadType(BreadTypeTestFixture.PASTRY).stickerNumber(1).name("단팥빵").imageUrl("a").build();
        Bread secondBread = Bread.builder().id(secondBreadId).breadType(BreadTypeTestFixture.PASTRY).stickerNumber(2).name("소금빵").imageUrl("b").build();

        when(authService.loginWithToss(request)).thenReturn(response);
        when(breadService.findAllSystemCatalogBreadsByIds(List.of(firstBreadId, secondBreadId)))
                .thenReturn(List.of(firstBread, secondBread));

        authComplexService.loginWithToss(request);

        verify(onboardingService).replaceSelectedBreads(userId, List.of(firstBread, secondBread));
    }


    @Test
    void loginWithTossRejectsNullSelectedBreadId() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TossLoginRequest request = new TossLoginRequest(
                "auth-code",
                "DEFAULT",
                java.util.Arrays.asList((UUID) null)
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

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authComplexService.loginWithToss(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(onboardingService, never()).replaceSelectedBreads(any(), any());
    }

    @Test
    void loginWithTossRejectsUnknownOnboardingBreadId() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID breadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        TossLoginRequest request = new TossLoginRequest(
                "auth-code",
                "DEFAULT",
                List.of(breadId)
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
        when(breadService.findAllSystemCatalogBreadsByIds(List.of(breadId)))
                .thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authComplexService.loginWithToss(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(onboardingService, never()).replaceSelectedBreads(any(), any());
    }

    private void verifyNoOnboardingInteractions() {
        verify(breadService, never()).findAllSystemCatalogBreadsByIds(any());
        verify(onboardingService, never()).replaceSelectedBreads(any(), any());
    }
}
