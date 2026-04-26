package com.bean.breaddiary.domain.user.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.TossWebhookRequest;
import com.bean.breaddiary.domain.auth.dto.response.TossWebhookResponse;
import com.bean.breaddiary.domain.auth.service.UserSessionService;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import com.bean.breaddiary.domain.user.dto.response.UserWithdrawalResponse;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import com.bean.breaddiary.global.logging.RequestLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWithdrawalService {

    private static final String WEBHOOK_REFERRER_UNLINK = "UNLINK";
    private static final String WEBHOOK_REFERRER_WITHDRAWAL_TERMS = "WITHDRAWAL_TERMS";
    private static final String WEBHOOK_REFERRER_WITHDRAWAL_TOSS = "WITHDRAWAL_TOSS";

    private final UserService userService;
    private final UserRepository userRepository;
    private final BreadRepository breadRepository;
    private final BreadRecordRepository breadRecordRepository;
    private final UserSessionService userSessionService;
    private final TossAuthClient tossAuthClient;

    @Value("${TOSS_WEBHOOK_SECRET:${app.auth.toss.webhook-secret:}}")
    private String tossWebhookSecret;

    @Transactional
    public UserWithdrawalResponse withdrawCurrentUser(UUID userId) {
        long startNanos = System.nanoTime();

        try {
            User user = userService.getUserById(userId);
            boolean tossLinked = StringUtils.hasText(user.getTossUserKey());

            if (tossLinked) {
                String tossAccessToken = resolveTossAccessToken(user);
                tossAuthClient.unlinkByUserKey(tossAccessToken, user.getTossUserKey());
            }

            hardDeleteUserData(user);

            log.info(
                    "USER action=withdrawal result=success requestId={} userId={} status={} tossLinked={} durationMs={}",
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    HttpStatus.OK.value(),
                    tossLinked,
                    durationMs(startNanos)
            );

            return new UserWithdrawalResponse(true);
        } catch (ResponseStatusException exception) {
            logUserFailure("withdrawal", userId, null, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedUserFailure("withdrawal", userId, null, exception, startNanos);
            throw exception;
        }
    }

    @Transactional
    public TossWebhookResponse handleTossWebhook(
            String requestWebhookSecret,
            TossWebhookRequest request
    ) {
        long startNanos = System.nanoTime();
        UUID userId = null;
        String referrer = request == null ? null : normalizeText(request.getReferrer());

        try {
            validateWebhookSecret(requestWebhookSecret);

            Optional<User> user = userRepository.findByTossUserKey(normalizeText(request.getUserKey()));
            if (user.isEmpty()) {
                logWebhookSuccess(resolveWebhookAction(referrer), null, referrer, false, startNanos);
                return new TossWebhookResponse(true, referrer);
            }

            userId = user.get().getId();

            if (WEBHOOK_REFERRER_UNLINK.equalsIgnoreCase(referrer)) {
                user.get().clearTossTokens();
                logWebhookSuccess("tossUnlinkWebhook", userId, referrer, true, startNanos);
                return new TossWebhookResponse(true, referrer);
            }

            if (WEBHOOK_REFERRER_WITHDRAWAL_TERMS.equalsIgnoreCase(referrer)
                    || WEBHOOK_REFERRER_WITHDRAWAL_TOSS.equalsIgnoreCase(referrer)) {
                hardDeleteUserData(user.get());
                logWebhookSuccess("tossWithdrawalWebhook", userId, referrer, true, startNanos);
                return new TossWebhookResponse(true, referrer);
            }

            logWebhookSuccess("tossUnknownWebhook", userId, referrer, true, startNanos);
            return new TossWebhookResponse(true, referrer);
        } catch (ResponseStatusException exception) {
            logUserFailure(resolveWebhookAction(referrer), userId, referrer, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedUserFailure(resolveWebhookAction(referrer), userId, referrer, exception, startNanos);
            throw exception;
        }
    }

    @Transactional
    public void hardDeleteUserData(User user) {
        UUID userId = user.getId();
        List<Bread> userCreatedBreads = breadRepository.findAllByCreatedBy(userId);

        breadRecordRepository.hardDeleteAllByUserId(userId);

        for (Bread bread : userCreatedBreads) {
            if (breadRecordRepository.existsByBread(bread)) {
                bread.clearCreator();
                continue;
            }

            breadRepository.delete(bread);
        }

        userSessionService.deleteAllSessions(userId);
        userRepository.delete(user);
    }

    private void validateWebhookSecret(String requestWebhookSecret) {
        String configuredSecret = normalizeText(tossWebhookSecret);
        String incomingSecret = decodeBasicAuthorization(requestWebhookSecret);

        if (!StringUtils.hasText(configuredSecret)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TOSS_WEBHOOK_SECRET 설정이 필요합니다."
            );
        }

        if (!configuredSecret.equals(incomingSecret)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "토스 웹훅 인증에 실패했습니다."
            );
        }
    }

    private String decodeBasicAuthorization(String authorizationHeader) {
        String normalizedAuthorization = normalizeText(authorizationHeader);
        if (!StringUtils.hasText(normalizedAuthorization)
                || !normalizedAuthorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "토스 웹훅 인증에 실패했습니다."
            );
        }

        String encodedCredentials = normalizedAuthorization.substring(6).trim();
        if (!StringUtils.hasText(encodedCredentials)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "토스 웹훅 인증에 실패했습니다."
            );
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
            String decodedCredentials = new String(decodedBytes, StandardCharsets.UTF_8);
            return normalizeText(decodedCredentials);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "토스 웹훅 인증에 실패했습니다.",
                    exception
            );
        }
    }

    private String requireTossRefreshToken(User user) {
        String refreshToken = normalizeText(user.getTossRefreshToken());
        if (!StringUtils.hasText(refreshToken)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "토스 RefreshToken 정보가 없어 회원 탈퇴를 진행할 수 없습니다."
            );
        }
        return refreshToken;
    }

    private String resolveTossAccessToken(User user) {
        if (hasUsableTossAccessToken(user)) {
            return user.getTossAccessToken().trim();
        }

        TossAuthClient.TossGenerateTokenSuccess refreshedToken =
                tossAuthClient.refreshAccessToken(requireTossRefreshToken(user));

        String refreshedAccessToken = requireText(
                refreshedToken.accessToken(),
                "토스 AccessToken 재발급에 실패했습니다."
        );
        String refreshedRefreshToken = normalizeText(refreshedToken.refreshToken());
        if (!StringUtils.hasText(refreshedRefreshToken)) {
            refreshedRefreshToken = user.getTossRefreshToken();
        }

        LocalDateTime accessTokenExpiresAt = refreshedToken.expiresIn() == null
                ? null
                : LocalDateTime.now().plusSeconds(refreshedToken.expiresIn());

        user.updateTossTokens(
                refreshedAccessToken,
                refreshedRefreshToken,
                accessTokenExpiresAt
        );

        return refreshedAccessToken;
    }

    private boolean hasUsableTossAccessToken(User user) {
        if (!StringUtils.hasText(user.getTossAccessToken())) {
            return false;
        }

        LocalDateTime expiresAt = user.getTossAccessTokenExpiresAt();
        if (expiresAt == null) {
            return true;
        }

        return expiresAt.isAfter(LocalDateTime.now());
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }
        return value.trim();
    }

    private void logWebhookSuccess(
            String action,
            UUID userId,
            String referrer,
            boolean userFound,
            long startNanos
    ) {
        log.info(
                "USER action={} result=success requestId={} userId={} status={} referrer={} userFound={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                HttpStatus.OK.value(),
                valueOrDefault(referrer),
                userFound,
                durationMs(startNanos)
        );
    }

    private void logUserFailure(
            String action,
            UUID userId,
            String referrer,
            ResponseStatusException exception,
            long startNanos
    ) {
        if (exception.getStatusCode().is5xxServerError()) {
            log.error(
                    "USER action={} result=fail requestId={} userId={} status={} referrer={} durationMs={}",
                    action,
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    exception.getStatusCode().value(),
                    valueOrDefault(referrer),
                    durationMs(startNanos),
                    exception
            );
            return;
        }

        log.warn(
                "USER action={} result=fail requestId={} userId={} status={} referrer={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                exception.getStatusCode().value(),
                valueOrDefault(referrer),
                durationMs(startNanos)
        );
    }

    private void logUnexpectedUserFailure(
            String action,
            UUID userId,
            String referrer,
            RuntimeException exception,
            long startNanos
    ) {
        log.error(
                "USER action={} result=fail requestId={} userId={} status={} referrer={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                "unexpected",
                valueOrDefault(referrer),
                durationMs(startNanos),
                exception
        );
    }

    private String resolveWebhookAction(String referrer) {
        if (WEBHOOK_REFERRER_UNLINK.equalsIgnoreCase(normalizeText(referrer))) {
            return "tossUnlinkWebhook";
        }
        if (WEBHOOK_REFERRER_WITHDRAWAL_TERMS.equalsIgnoreCase(normalizeText(referrer))
                || WEBHOOK_REFERRER_WITHDRAWAL_TOSS.equalsIgnoreCase(normalizeText(referrer))) {
            return "tossWithdrawalWebhook";
        }
        return "tossWithdrawalWebhook";
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String valueOrDefault(Object value) {
        return value == null ? "-" : value.toString();
    }
}
