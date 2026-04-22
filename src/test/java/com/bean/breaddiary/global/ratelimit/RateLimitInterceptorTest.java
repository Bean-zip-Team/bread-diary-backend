package com.bean.breaddiary.global.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    private InMemoryRateLimiter rateLimiter;
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(InMemoryRateLimiter.class);
        rateLimitInterceptor = new RateLimitInterceptor(rateLimiter);
    }

    @Test
    void preHandleBlocksTossLoginWhenLimitExceeded() {
        when(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1"))
                .thenReturn(new InMemoryRateLimiter.RateLimitResult(false, 30));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitInterceptor.preHandle(request("POST", "/auth/toss"), new MockHttpServletResponse(), new Object())
        );

        assertEquals(RateLimitPolicy.AUTH_TOSS, exception.getPolicy());
        assertEquals(30, exception.getRetryAfterSeconds());
    }

    @Test
    void preHandleBlocksRefreshWhenLimitExceeded() {
        when(rateLimiter.consume(RateLimitPolicy.AUTH_REFRESH, "127.0.0.1"))
                .thenReturn(new InMemoryRateLimiter.RateLimitResult(false, 20));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitInterceptor.preHandle(request("POST", "/auth/refresh"), new MockHttpServletResponse(), new Object())
        );

        assertEquals(RateLimitPolicy.AUTH_REFRESH, exception.getPolicy());
        assertEquals(20, exception.getRetryAfterSeconds());
    }

    @Test
    void preHandleBlocksTossWebhookWhenLimitExceeded() {
        when(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS_WEBHOOK, "127.0.0.1"))
                .thenReturn(new InMemoryRateLimiter.RateLimitResult(false, 10));

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitInterceptor.preHandle(request("POST", "/auth/webhook/toss-unlink"), new MockHttpServletResponse(), new Object())
        );

        assertEquals(RateLimitPolicy.AUTH_TOSS_WEBHOOK, exception.getPolicy());
        assertEquals(10, exception.getRetryAfterSeconds());
    }

    @Test
    void preHandleAllowsTargetEndpointWithinLimitAndUsesRemoteAddrIdentifier() {
        when(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "203.0.113.10"))
                .thenReturn(new InMemoryRateLimiter.RateLimitResult(true, 0));

        MockHttpServletRequest request = request("POST", "/auth/toss");
        request.setRemoteAddr("203.0.113.10");

        assertTrue(rateLimitInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verify(rateLimiter).consume(RateLimitPolicy.AUTH_TOSS, "203.0.113.10");
    }

    @Test
    void preHandleSkipsNonTargetEndpoint() {
        assertTrue(rateLimitInterceptor.preHandle(request("POST", "/auth/logout"), new MockHttpServletResponse(), new Object()));

        verify(rateLimiter, never()).consume(any(RateLimitPolicy.class), any(String.class));
    }

    @Test
    void preHandleFailsOpenWhenLimiterThrowsUnexpectedException() {
        when(rateLimiter.consume(eq(RateLimitPolicy.AUTH_TOSS), any(String.class)))
                .thenThrow(new IllegalStateException("internal limiter failure"));

        assertTrue(rateLimitInterceptor.preHandle(request("POST", "/auth/toss"), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
