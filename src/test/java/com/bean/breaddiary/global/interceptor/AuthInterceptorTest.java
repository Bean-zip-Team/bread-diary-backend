package com.bean.breaddiary.global.interceptor;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.service.JwtTokenProvider;
import com.bean.breaddiary.domain.auth.service.UserSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private UserSessionService userSessionService;
    private JwtTokenProvider jwtTokenProvider;
    private AuthInterceptor authInterceptor;
    private HandlerMethod handlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        userSessionService = mock(UserSessionService.class);
        jwtTokenProvider = new JwtTokenProvider(
                new ObjectMapper(),
                "test-secret-key",
                "bread-diary"
        );
        authInterceptor = new AuthInterceptor(jwtTokenProvider, userSessionService);

        Method handleMethod = DummyController.class.getDeclaredMethod("handle");
        handlerMethod = new HandlerMethod(new DummyController(), handleMethod);
    }

    @Test
    void preHandleSkipsWhitelistedBreadTypesEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bread-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(authInterceptor.preHandle(request, response, handlerMethod));
        verifyNoInteractions(userSessionService);
    }

    @Test
    void preHandleRejectsMissingBearerTokenForProtectedEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/breads/550e8400-e29b-41d4-a716-446655440000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authInterceptor.preHandle(request, response, handlerMethod)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void preHandleRejectsRefreshTokenOnProtectedEndpoint() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        String refreshToken = jwtTokenProvider.createRefreshToken(
                userId,
                sessionId,
                "refresh-jti",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        MockHttpServletRequest request = authorizedRequest(
                "GET",
                "/breads/550e8400-e29b-41d4-a716-446655440000",
                refreshToken
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authInterceptor.preHandle(request, response, handlerMethod)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void preHandleRejectsWhenSessionIsNotActive() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
        String accessToken = jwtTokenProvider.createAccessToken(
                userId,
                sessionId,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        when(userSessionService.findActiveSession(eq(sessionId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = authorizedRequest("POST", "/breads", accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authInterceptor.preHandle(request, response, handlerMethod)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void preHandleStoresAuthenticatedUserAttributesForValidAccessToken() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440021");
        LocalDateTime now = LocalDateTime.now();
        String accessToken = jwtTokenProvider.createAccessToken(
                userId,
                sessionId,
                now,
                now.plusDays(1)
        );

        UserSession userSession = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .refreshTokenHash("hashed-refresh-token")
                .currentJti("current-jti")
                .refreshExpiresAt(now.plusDays(30))
                .build();

        when(userSessionService.findActiveSession(eq(sessionId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(userSession));

        MockHttpServletRequest request = authorizedRequest(
                "GET",
                "/breads/550e8400-e29b-41d4-a716-446655440020",
                accessToken
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(authInterceptor.preHandle(request, response, handlerMethod));
        assertEquals(userId, request.getAttribute(AuthRequestAttributes.USER_ID));
        assertEquals(sessionId, request.getAttribute(AuthRequestAttributes.SESSION_ID));
    }

    private MockHttpServletRequest authorizedRequest(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        public void handle() {
        }
    }
}
