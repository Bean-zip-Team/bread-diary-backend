package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.dto.response.LogoutResponse;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.bean.breaddiary.global.logging.RequestLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String PENDING_REFRESH_TOKEN_HASH = "__PENDING_REFRESH_TOKEN__";
    private static final int NICKNAME_MAX_LENGTH = 30;

    private final UserRepository userRepository;
    private final UserSessionService userSessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TossAuthClient tossAuthClient;
    private final TossUserInfoDecryptor tossUserInfoDecryptor;

    @Transactional
    public AuthTokenResponse loginWithToss(TossLoginRequest request) {
        long startNanos = System.nanoTime();
        UUID userId = null;
        UUID sessionId = null;

        try {
            LocalDateTime issuedAt = LocalDateTime.now();

            TossAuthClient.TossGenerateTokenSuccess tossToken =
                    tossAuthClient.exchangeAuthorizationCode(
                            request.getAuthorizationCode(),
                            request.getReferrer()
                    );
            TossAuthClient.TossLoginMeSuccess tossUserInfo =
                    tossAuthClient.getUserInfo(tossToken.accessToken());

            ResolvedTossProfile tossProfile = resolveTossProfile(tossUserInfo);
            UserResolution userResolution = findOrCreateUser(tossProfile);
            updateTossTokens(userResolution.user(), tossToken, issuedAt);
            userId = userResolution.user().getId();

            UserSession userSession = createPendingSession(userResolution.user(), issuedAt);
            sessionId = userSession.getId();

            AuthTokenResponse response = issueTokens(
                    userResolution.user(),
                    userSession,
                    userResolution.newUser(),
                    issuedAt
            );

            log.info(
                    "AUTH action=login result=success requestId={} userId={} sessionId={} status={} newUser={} durationMs={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    valueOrDefault(sessionId),
                    HttpStatus.OK.value(),
                    userResolution.newUser(),
                    durationMs(startNanos)
            );

            return response;
        } catch (ResponseStatusException exception) {
            logAuthFailure("login", userId, sessionId, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedAuthFailure("login", userId, sessionId, exception, startNanos);
            throw exception;
        }
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        long startNanos = System.nanoTime();
        UUID userId = null;
        UUID sessionId = null;

        try {
            String refreshToken = requireText(request.getRefreshToken(), "리프레시 토큰이 필요합니다.");
            JwtTokenProvider.JwtTokenClaims claims = parseRefreshToken(refreshToken);
            userId = claims.userId();
            sessionId = claims.sessionId();
            LocalDateTime now = LocalDateTime.now();

            UserSession userSession = userSessionService.findSessionForUpdate(claims.sessionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "유효한 세션이 없습니다."
                    ));

            validateRefreshTokenState(userSession, claims, refreshToken, now, true);

            User user = getActiveUser(userSession.getUserId());

            AuthTokenResponse response = issueTokens(user, userSession, false, now);

            log.info(
                    "AUTH action=refresh result=success requestId={} userId={} sessionId={} status={} durationMs={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(user.getId()),
                    valueOrDefault(userSession.getId()),
                    HttpStatus.OK.value(),
                    durationMs(startNanos)
            );

            return response;
        } catch (ResponseStatusException exception) {
            logAuthFailure("refresh", userId, sessionId, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedAuthFailure("refresh", userId, sessionId, exception, startNanos);
            throw exception;
        }
    }

    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        long startNanos = System.nanoTime();
        UUID userId = null;
        UUID sessionId = null;

        try {
            String refreshToken = requireText(request.getRefreshToken(), "리프레시 토큰이 필요합니다.");
            JwtTokenProvider.JwtTokenClaims claims = parseRefreshToken(refreshToken);
            userId = claims.userId();
            sessionId = claims.sessionId();
            LocalDateTime now = LocalDateTime.now();

            UserSession userSession = userSessionService.findSessionForUpdate(claims.sessionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "로그아웃할 세션이 없습니다."
                    ));

            validateRefreshTokenState(userSession, claims, refreshToken, now, false);
            userSessionService.revokeSession(userSession, now);

            log.info(
                    "AUTH action=logout result=success requestId={} userId={} sessionId={} status={} durationMs={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userSession.getUserId()),
                    valueOrDefault(userSession.getId()),
                    HttpStatus.OK.value(),
                    durationMs(startNanos)
            );

            return new LogoutResponse(true);
        } catch (ResponseStatusException exception) {
            logAuthFailure("logout", userId, sessionId, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedAuthFailure("logout", userId, sessionId, exception, startNanos);
            throw exception;
        }
    }

    @Transactional
    public LogoutResponse logoutCurrentSession(UUID sessionId) {
        long startNanos = System.nanoTime();
        UUID userId = null;

        try {
            LocalDateTime now = LocalDateTime.now();

            UserSession userSession = userSessionService.findSessionForUpdate(sessionId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "로그아웃할 세션이 없습니다."
                    ));

            userId = userSession.getUserId();
            userSessionService.revokeSession(userSession, now);

            log.info(
                    "AUTH action=logoutCurrentSession result=success requestId={} userId={} sessionId={} status={} durationMs={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    valueOrDefault(userSession.getId()),
                    HttpStatus.OK.value(),
                    durationMs(startNanos)
            );

            return new LogoutResponse(true);
        } catch (ResponseStatusException exception) {
            logAuthFailure("logoutCurrentSession", userId, sessionId, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedAuthFailure("logoutCurrentSession", userId, sessionId, exception, startNanos);
            throw exception;
        }
    }

    private AuthTokenResponse issueTokens(
            User user,
            UserSession userSession,
            boolean newUser,
            LocalDateTime issuedAt
    ) {
        String refreshJti = UUID.randomUUID().toString();
        LocalDateTime accessTokenExpiresAt = userSessionService.calculateAccessTokenExpiresAt(issuedAt);
        LocalDateTime refreshTokenExpiresAt = userSessionService.calculateRefreshTokenExpiresAt(issuedAt);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                userSession.getId(),
                issuedAt,
                accessTokenExpiresAt
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(),
                userSession.getId(),
                refreshJti,
                issuedAt,
                refreshTokenExpiresAt
        );

        userSessionService.rotateRefreshToken(
                userSession,
                hashRefreshToken(refreshToken),
                refreshJti,
                issuedAt
        );

        return new AuthTokenResponse(
                user.getId(),
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                newUser
        );
    }

    private UserSession createPendingSession(User user, LocalDateTime issuedAt) {
        return userSessionService.createSession(
                user.getId(),
                PENDING_REFRESH_TOKEN_HASH,
                UUID.randomUUID().toString(),
                issuedAt
        );
    }

    private ResolvedTossProfile resolveTossProfile(TossAuthClient.TossLoginMeSuccess tossUserInfo) {
        String tossUserKey = requireText(
                tossUserInfo.userKey() == null ? null : String.valueOf(tossUserInfo.userKey()),
                "토스 사용자 키가 없습니다."
        );
        String decryptedName = tossUserInfoDecryptor.decryptNullable(tossUserInfo.name());
        String decryptedEmail = normalizeNullable(tossUserInfoDecryptor.decryptNullable(tossUserInfo.email()));

        return new ResolvedTossProfile(
                tossUserKey,
                resolveNickname(decryptedName, null, tossUserKey),
                decryptedEmail
        );
    }

    private UserResolution findOrCreateUser(ResolvedTossProfile tossProfile) {
        Optional<User> userByTossUserKey = userRepository.findByTossUserKey(tossProfile.tossUserKey());
        if (userByTossUserKey.isPresent()) {
            User user = userByTossUserKey.get();
            synchronizeUser(user, tossProfile);
            return new UserResolution(user, false);
        }

        if (StringUtils.hasText(tossProfile.email())) {
            Optional<User> userByEmail = userRepository.findByEmail(tossProfile.email());
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                synchronizeUser(user, tossProfile);
                return new UserResolution(user, false);
            }
        }

        User user = userRepository.save(
                        User.builder()
                                .tossUserKey(tossProfile.tossUserKey())
                                .tossRefreshToken(null)
                                .nickname(tossProfile.nickname())
                                .email(tossProfile.email())
                                .build()
        );

        return new UserResolution(user, true);
    }

    private void synchronizeUser(User user, ResolvedTossProfile tossProfile) {
        String nickname = resolveNickname(
                tossProfile.nickname(),
                user.getNickname(),
                tossProfile.tossUserKey()
        );
        String email = resolveEmail(tossProfile.email(), user.getEmail());

        if (user.isDeleted()) {
            user.activate(tossProfile.tossUserKey(), nickname, email);
            return;
        }

        user.updateFromToss(tossProfile.tossUserKey(), nickname, email);
    }

    private User getActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "활성 사용자 정보를 찾을 수 없습니다."
                ));
    }

    private JwtTokenProvider.JwtTokenClaims parseRefreshToken(String refreshToken) {
        JwtTokenProvider.JwtTokenClaims claims = jwtTokenProvider.parseToken(refreshToken);

        if (!REFRESH_TOKEN_TYPE.equals(claims.type())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "리프레시 토큰이 아닙니다."
            );
        }

        if (!StringUtils.hasText(claims.jti())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "리프레시 토큰 jti가 없습니다."
            );
        }

        return claims;
    }

    private void validateRefreshTokenState(
            UserSession userSession,
            JwtTokenProvider.JwtTokenClaims claims,
            String refreshToken,
            LocalDateTime now,
            boolean requireNotExpired
    ) {
        if (!userSession.matchesCurrentJti(claims.jti())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 회전된 리프레시 토큰입니다."
            );
        }

        if (!userSession.matchesRefreshTokenHash(hashRefreshToken(refreshToken))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "리프레시 토큰이 유효하지 않습니다."
            );
        }

        if (requireNotExpired && (claims.isExpiredAt(now) || userSession.isRefreshExpiredAt(now))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "리프레시 토큰이 만료되었습니다."
            );
        }
    }

    private String resolveEmail(String tossEmail, String existingEmail) {
        if (StringUtils.hasText(tossEmail)) {
            return tossEmail;
        }

        return normalizeNullable(existingEmail);
    }

    private String resolveNickname(
            String tossName,
            String existingNickname,
            String tossUserKey
    ) {
        if (StringUtils.hasText(tossName)) {
            return truncate(tossName.trim());
        }

        if (StringUtils.hasText(existingNickname)) {
            return truncate(existingNickname.trim());
        }

        String suffix = tossUserKey.length() > 8
                ? tossUserKey.substring(tossUserKey.length() - 8)
                : tossUserKey;

        return truncate("toss-" + suffix);
    }

    private String truncate(String value) {
        return value.length() <= NICKNAME_MAX_LENGTH
                ? value
                : value.substring(0, NICKNAME_MAX_LENGTH);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void updateTossTokens(
            User user,
            TossAuthClient.TossGenerateTokenSuccess tossToken,
            LocalDateTime issuedAt
    ) {
        String accessToken = normalizeNullable(tossToken.accessToken());
        String refreshToken = normalizeNullable(tossToken.refreshToken());
        LocalDateTime accessTokenExpiresAt = tossToken.expiresIn() == null
                ? null
                : issuedAt.plusSeconds(tossToken.expiresIn());

        user.updateTossTokens(accessToken, refreshToken, accessTokenExpiresAt);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", exception);
        }
    }

    private void logAuthFailure(
            String action,
            UUID userId,
            UUID sessionId,
            ResponseStatusException exception,
            long startNanos
    ) {
        if (exception.getStatusCode().is5xxServerError()) {
            log.error(
                    "AUTH action={} result=fail requestId={} userId={} sessionId={} status={} exceptionType={} durationMs={}",
                    action,
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    valueOrDefault(sessionId),
                    exception.getStatusCode().value(),
                    exception.getClass().getSimpleName(),
                    durationMs(startNanos)
            );
            return;
        }

        log.warn(
                "AUTH action={} result=fail requestId={} userId={} sessionId={} status={} exceptionType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                valueOrDefault(sessionId),
                exception.getStatusCode().value(),
                exception.getClass().getSimpleName(),
                durationMs(startNanos)
        );
    }

    private void logUnexpectedAuthFailure(
            String action,
            UUID userId,
            UUID sessionId,
            RuntimeException exception,
            long startNanos
    ) {
        log.error(
                "AUTH action={} result=fail requestId={} userId={} sessionId={} status={} exceptionType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                valueOrDefault(sessionId),
                "unexpected",
                exception.getClass().getSimpleName(),
                durationMs(startNanos)
        );
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String valueOrDefault(Object value) {
        return value == null ? "-" : value.toString();
    }

    private record ResolvedTossProfile(
            String tossUserKey,
            String nickname,
            String email
    ) {
    }

    private record UserResolution(
            User user,
            boolean newUser
    ) {
    }
}
