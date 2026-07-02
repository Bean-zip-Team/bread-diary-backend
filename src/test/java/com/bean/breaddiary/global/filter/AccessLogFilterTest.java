package com.bean.breaddiary.global.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import com.bean.breaddiary.global.logging.RequestLogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessLogFilterTest {

    private AccessLogFilter accessLogFilter;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        accessLogFilter = new AccessLogFilter();
        logger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void doFilterWritesMethodPathStatusDurationRequestIdAndClientIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("X-Request-Id", "front-request-1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) -> response.setStatus(200);

        accessLogFilter.doFilter(request, response, filterChain);

        String accessLog = singleLogMessage();
        assertTrue(accessLog.startsWith("ACCESS "));
        assertTrue(accessLog.contains("method=GET"));
        assertTrue(accessLog.contains("path=/users/me"));
        assertTrue(accessLog.contains("status=200"));
        assertTrue(accessLog.matches(".*durationMs=\\d+.*"));
        assertTrue(accessLog.contains("requestId=front-request-1"));
        assertTrue(accessLog.contains("clientIp=203.0.113.10"));
        assertEquals("front-request-1", response.getHeader(RequestLogContext.REQUEST_ID_HEADER));
        assertEquals("front-request-1", request.getAttribute(RequestLogContext.REQUEST_ID_ATTRIBUTE));
    }

    @Test
    void doFilterSanitizesRequestIdForResponseAttributeAndAccessLog() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("X-Request-Id", " front request!\r\nid:1 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessLogFilter.doFilter(request, response, passThroughChain());

        String expectedRequestId = "frontrequestid:1";
        String accessLog = singleLogMessage();
        assertEquals(expectedRequestId, response.getHeader(RequestLogContext.REQUEST_ID_HEADER));
        assertEquals(expectedRequestId, request.getAttribute(RequestLogContext.REQUEST_ID_ATTRIBUTE));
        assertTrue(accessLog.contains("requestId=" + expectedRequestId));
    }

    @Test
    void doFilterGeneratesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessLogFilter.doFilter(request, response, passThroughChain());

        String responseRequestId = response.getHeader(RequestLogContext.REQUEST_ID_HEADER);
        UUID.fromString(responseRequestId);

        String accessLog = singleLogMessage();
        assertEquals(responseRequestId, request.getAttribute(RequestLogContext.REQUEST_ID_ATTRIBUTE));
        assertTrue(accessLog.contains("requestId=" + responseRequestId));
    }

    @Test
    void doFilterWritesAuthenticatedUserAttributesWhenAvailable() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/logout");
        request.setAttribute(AuthRequestAttributes.USER_ID, userId);
        request.setAttribute(AuthRequestAttributes.SESSION_ID, sessionId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessLogFilter.doFilter(request, response, passThroughChain());

        String accessLog = singleLogMessage();
        assertTrue(accessLog.contains("userId=" + userId));
        assertTrue(accessLog.contains("sessionId=" + sessionId));
    }

    @Test
    void doFilterUsesSafeDefaultsWhenAuthenticatedAttributesAreMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bread-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessLogFilter.doFilter(request, response, passThroughChain());

        String accessLog = singleLogMessage();
        assertTrue(accessLog.contains("userId=-"));
        assertTrue(accessLog.contains("sessionId=-"));
    }

    @Test
    void doFilterDoesNotLogSensitiveHeadersOrQueryString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/toss");
        request.setQueryString("authorizationCode=secret-code&refreshToken=secret-refresh&uploadUrl=https://s3.example.com/file?X-Amz-Signature=secret-signature");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer secret-access-token");
        request.addHeader("x-toss-webhook-secret", "secret-webhook");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessLogFilter.doFilter(request, response, passThroughChain());

        String accessLog = singleLogMessage();
        assertTrue(accessLog.contains("path=/auth/toss"));
        assertFalse(accessLog.contains("secret-access-token"));
        assertFalse(accessLog.contains("secret-webhook"));
        assertFalse(accessLog.contains("secret-code"));
        assertFalse(accessLog.contains("secret-refresh"));
        assertFalse(accessLog.contains("X-Amz-Signature"));
        assertFalse(accessLog.contains("s3.example.com"));
    }

    @Test
    void doFilterWritesAccessLogAndRethrowsWhenChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> accessLogFilter.doFilter(request, response, filterChain));

        String accessLog = singleLogMessage();
        assertTrue(accessLog.contains("method=GET"));
        assertTrue(accessLog.contains("path=/users/me"));
        assertTrue(accessLog.contains("status=500"));
    }

    @Test
    void doFilterSkipsExcludedPaths() throws Exception {
        AtomicInteger chainCount = new AtomicInteger();
        FilterChain countingChain = (servletRequest, servletResponse) -> chainCount.incrementAndGet();

        accessLogFilter.doFilter(new MockHttpServletRequest("OPTIONS", "/users/me"), new MockHttpServletResponse(), countingChain);
        accessLogFilter.doFilter(new MockHttpServletRequest("GET", "/swagger-ui/index.html"), new MockHttpServletResponse(), countingChain);
        accessLogFilter.doFilter(new MockHttpServletRequest("GET", "/v3/api-docs"), new MockHttpServletResponse(), countingChain);
        accessLogFilter.doFilter(new MockHttpServletRequest("GET", "/favicon.ico"), new MockHttpServletResponse(), countingChain);
        accessLogFilter.doFilter(new MockHttpServletRequest("GET", "/assets/app.js"), new MockHttpServletResponse(), countingChain);

        assertEquals(5, chainCount.get());
        assertTrue(listAppender.list.isEmpty());
    }

    private FilterChain passThroughChain() {
        return (servletRequest, servletResponse) -> {
        };
    }

    private String singleLogMessage() {
        assertEquals(1, listAppender.list.size());
        return listAppender.list.get(0).getFormattedMessage();
    }
}
