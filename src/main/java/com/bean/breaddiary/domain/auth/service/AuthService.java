package com.bean.breaddiary.domain.auth.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.entity.UserSession;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

        String refreshJti = UUID.randomUUID().toString();
        UserSession userSession = userSessionService.createSession(
                userResolution.user().getId(),
                PENDING_REFRESH_TOKEN_HASH,
                refreshJti,
                issuedAt
        );

        LocalDateTime accessTokenExpiresAt = userSessionService.calculateAccessTokenExpiresAt(issuedAt);
        LocalDateTime refreshTokenExpiresAt = userSessionService.calculateRefreshTokenExpiresAt(issuedAt);

        String accessToken = jwtTokenProvider.createAccessToken(
                userResolution.user().getId(),
                userSession.getId(),
                issuedAt,
                accessTokenExpiresAt
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                userResolution.user().getId(),
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
                userResolution.user().getId(),
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                userResolution.newUser()
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
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    message
            );
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
