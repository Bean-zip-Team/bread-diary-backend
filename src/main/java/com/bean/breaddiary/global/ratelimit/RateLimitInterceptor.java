package com.bean.breaddiary.global.ratelimit;

import com.bean.breaddiary.global.logging.RequestLogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String DEFAULT_IDENTIFIER = "-";

    private final InMemoryRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return RateLimitPolicy.find(request.getMethod(), request.getRequestURI())
                .map(policy -> checkRateLimit(policy, request))
                .orElse(true);
    }

    private boolean checkRateLimit(RateLimitPolicy policy, HttpServletRequest request) {
        String identifier = resolveIdentifier(request);

        try {
            InMemoryRateLimiter.RateLimitResult result = rateLimiter.consume(policy, identifier);
            if (result.allowed()) {
                return true;
            }

            log.warn(
                    "AUTH action=authRateLimit result=blocked requestId={} endpoint={} identifier={} limit={} windowSeconds={} retryAfterSeconds={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    policy.path(),
                    identifier,
                    policy.limit(),
                    policy.windowSeconds(),
                    result.retryAfterSeconds()
            );
            throw new RateLimitExceededException(policy, result.retryAfterSeconds());
        } catch (RateLimitExceededException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "AUTH action=authRateLimit result=error requestId={} endpoint={} exceptionType={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    policy.path(),
                    exception.getClass().getSimpleName()
            );
            return true;
        }
    }

    private String resolveIdentifier(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : DEFAULT_IDENTIFIER;
    }
}
