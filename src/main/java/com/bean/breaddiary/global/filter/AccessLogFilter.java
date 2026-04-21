package com.bean.breaddiary.global.filter;

import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final String ACCESS_LOG_PREFIX = "ACCESS";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String DEFAULT_VALUE = "-";
    private static final int MAX_REQUEST_ID_LENGTH = 80;
    private static final int MAX_CLIENT_IP_LENGTH = 80;
    private static final int INTERNAL_SERVER_ERROR_STATUS = 500;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Pattern UNSAFE_REQUEST_ID_CHARACTERS = Pattern.compile("[^A-Za-z0-9._:-]");
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\r\\n\\t]");
    private static final Pattern STATIC_RESOURCE_PATTERN = Pattern.compile(
            ".+\\.(css|js|map|png|jpg|jpeg|gif|svg|ico|webp|woff|woff2|ttf)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        return "OPTIONS".equalsIgnoreCase(method)
                || "/error".equals(path)
                || "/swagger-ui.html".equals(path)
                || PATH_MATCHER.match("/swagger-ui/**", path)
                || PATH_MATCHER.match("/v3/api-docs/**", path)
                || PATH_MATCHER.match("/webjars/**", path)
                || "/favicon.ico".equals(path)
                || STATIC_RESOURCE_PATTERN.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        boolean failed = false;

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            writeAccessLog(request, response, startNanos, failed);
        }
    }

    private void writeAccessLog(
            HttpServletRequest request,
            HttpServletResponse response,
            long startNanos,
            boolean failed
    ) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        int status = resolveStatus(response, failed);

        log.info(
                "{} method={} path={} status={} durationMs={} userId={} sessionId={} requestId={} clientIp={}",
                ACCESS_LOG_PREFIX,
                request.getMethod(),
                request.getRequestURI(),
                status,
                durationMs,
                resolveAttribute(request, AuthRequestAttributes.USER_ID),
                resolveAttribute(request, AuthRequestAttributes.SESSION_ID),
                resolveRequestId(request),
                resolveClientIp(request)
        );
    }

    private int resolveStatus(HttpServletResponse response, boolean failed) {
        int status = response.getStatus();

        if (failed && status < INTERNAL_SERVER_ERROR_STATUS) {
            return INTERNAL_SERVER_ERROR_STATUS;
        }

        return status;
    }

    private String resolveAttribute(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);

        if (value == null) {
            return DEFAULT_VALUE;
        }

        return sanitize(value.toString(), MAX_REQUEST_ID_LENGTH);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (!StringUtils.hasText(requestId)) {
            return UUID.randomUUID().toString();
        }

        String sanitized = UNSAFE_REQUEST_ID_CHARACTERS.matcher(requestId.trim()).replaceAll("");

        if (!StringUtils.hasText(sanitized)) {
            return UUID.randomUUID().toString();
        }

        return truncate(sanitized, MAX_REQUEST_ID_LENGTH);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);

        if (StringUtils.hasText(forwardedFor)) {
            return sanitize(forwardedFor.split(",", 2)[0], MAX_CLIENT_IP_LENGTH);
        }

        return sanitize(request.getRemoteAddr(), MAX_CLIENT_IP_LENGTH);
    }

    private String sanitize(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_VALUE;
        }

        String sanitized = CONTROL_CHARACTERS.matcher(value.trim()).replaceAll("");

        if (!StringUtils.hasText(sanitized)) {
            return DEFAULT_VALUE;
        }

        return truncate(sanitized, maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
