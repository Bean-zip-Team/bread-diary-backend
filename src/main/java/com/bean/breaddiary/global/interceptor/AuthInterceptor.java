package com.bean.breaddiary.global.interceptor;

import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.auth.service.JwtTokenProvider;
import com.bean.breaddiary.domain.auth.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String HTTP_GET = "GET";
    private static final String HTTP_OPTIONS = "OPTIONS";
    private static final String HTTP_POST = "POST";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSessionService userSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!requiresAuthentication(request)) {
            return true;
        }

        String accessToken = resolveAccessToken(request);
        LocalDateTime now = LocalDateTime.now();
        JwtTokenProvider.JwtTokenClaims claims = jwtTokenProvider.parseToken(accessToken);

        validateAccessToken(claims, now);

        UserSession userSession = userSessionService.findActiveSession(claims.sessionId(), now)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "유효한 세션이 없습니다."
                ));

        if (!userSession.getUserId().equals(claims.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "세션 사용자 정보가 올바르지 않습니다."
            );
        }

        request.setAttribute(AuthRequestAttributes.USER_ID, claims.userId());
        request.setAttribute(AuthRequestAttributes.SESSION_ID, claims.sessionId());
        return true;
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String method = request.getMethod();
        String requestUri = request.getRequestURI();

        if (HTTP_OPTIONS.equalsIgnoreCase(method)) {
            return false;
        }

        if ("/error".equals(requestUri)
                || "/swagger-ui.html".equals(requestUri)
                || PATH_MATCHER.match("/swagger-ui/**", requestUri)
                || PATH_MATCHER.match("/v3/api-docs/**", requestUri)) {
            return false;
        }

        if (HTTP_POST.equalsIgnoreCase(method)
                && ("/auth/toss".equals(requestUri)
                || "/auth/refresh".equals(requestUri)
                || "/auth/webhook/toss-unlink".equals(requestUri))) {
            return false;
        }

        if (HTTP_GET.equalsIgnoreCase(method) && "/bread-types".equals(requestUri)) {
            return false;
        }

        if (HTTP_GET.equalsIgnoreCase(method)
                && ("/breads".equals(requestUri)
                || "/breads/autocomplete".equals(requestUri)
                || PATH_MATCHER.match("/breads/catalog/*", requestUri))) {
            return false;
        }

        return true;
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bearer Access Token이 필요합니다."
            );
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(accessToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bearer Access Token이 비어 있습니다."
            );
        }

        return accessToken;
    }

    private void validateAccessToken(JwtTokenProvider.JwtTokenClaims claims, LocalDateTime now) {
        if (!ACCESS_TOKEN_TYPE.equals(claims.type())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Access Token만 사용할 수 있습니다."
            );
        }

        if (claims.isExpiredAt(now)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Access Token이 만료되었습니다."
            );
        }
    }
}
