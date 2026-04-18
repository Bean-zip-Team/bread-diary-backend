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
import lombok.RequiredArgsConstructor;
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

        UserSession userSession = createPendingSession(userResolution.user(), issuedAt);

        return issueTokens(
                userResolution.user(),
                userSession,
                userResolution.newUser(),
                issuedAt
        );
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = requireText(request.getRefreshToken(), "리프레시 토큰이 필요합니다.");
        JwtTokenProvider.JwtTokenClaims claims = parseRefreshToken(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        UserSession userSession = userSessionService.findSessionForUpdate(claims.sessionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "유효한 세션이 없습니다."
                ));

        validateRefreshTokenState(userSession, claims, refreshToken, now, true);

        User user = getActiveUser(userSession.getUserId());

        return issueTokens(user, userSession, false, now);
    }

    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        String refreshToken = requireText(request.getRefreshToken(), "리프레시 토큰이 필요합니다.");
        JwtTokenProvider.JwtTokenClaims claims = parseRefreshToken(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        UserSession userSession = userSessionService.findSessionForUpdate(claims.sessionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그아웃할 세션이 없습니다."
                ));

        validateRefreshTokenState(userSession, claims, refreshToken, now, false);
        userSessionService.revokeSession(userSession, now);

        return new LogoutResponse(true);
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
                tossUserInfo.userKey() == null ? null : tossUserInfo.userKey().asText(null),
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
