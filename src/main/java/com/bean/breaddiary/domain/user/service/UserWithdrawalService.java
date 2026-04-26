package com.bean.breaddiary.domain.user.service;

import com.bean.breaddiary.domain.auth.client.TossAuthClient;
import com.bean.breaddiary.domain.auth.dto.request.TossWebhookRequest;
import com.bean.breaddiary.domain.auth.dto.response.TossWebhookResponse;
import com.bean.breaddiary.domain.auth.entity.TossWebhookEventType;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWithdrawalService {

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
                TossAuthClient.TossGenerateTokenSuccess refreshedToken =
                        tossAuthClient.refreshAccessToken(requireTossRefreshToken(user));
                user.updateTossRefreshToken(normalizeText(refreshedToken.refreshToken()));
                tossAuthClient.unlinkByUserKey(
                        requireText(refreshedToken.accessToken(), "토스 AccessToken 재발급에 실패했습니다."),
                        user.getTossUserKey()
                );
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
        TossWebhookEventType eventType = request == null ? null : request.getEventType();

        try {
            validateWebhookSecret(requestWebhookSecret);

            Optional<User> user = userRepository.findByTossUserKey(normalizeText(request.getUserKey()));
            if (user.isEmpty()) {
                logWebhookSuccess(resolveWebhookAction(eventType), null, eventType, false, startNanos);
                return new TossWebhookResponse(true, request.getEventType());
            }

            userId = user.get().getId();

            if (request.getEventType() == TossWebhookEventType.UNLINK) {
                clearSessionsOnly(userId);
                logWebhookSuccess("tossUnlinkWebhook", userId, eventType, true, startNanos);
                return new TossWebhookResponse(true, request.getEventType());
            }

            hardDeleteUserData(user.get());
            logWebhookSuccess("tossWithdrawalWebhook", userId, eventType, true, startNanos);
            return new TossWebhookResponse(true, request.getEventType());
        } catch (ResponseStatusException exception) {
            logUserFailure(resolveWebhookAction(eventType), userId, eventType, exception, startNanos);
            throw exception;
        } catch (RuntimeException exception) {
            logUnexpectedUserFailure(resolveWebhookAction(eventType), userId, eventType, exception, startNanos);
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

    @Transactional
    public void clearSessionsOnly(UUID userId) {
        userSessionService.deleteAllSessions(userId);
    }

    private void validateWebhookSecret(String requestWebhookSecret) {
        String configuredSecret = normalizeText(tossWebhookSecret);
        String incomingSecret = normalizeText(requestWebhookSecret);

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

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }
        return value.trim();
    }

    private void logWebhookSuccess(
            String action,
            UUID userId,
            TossWebhookEventType eventType,
            boolean userFound,
            long startNanos
    ) {
        log.info(
                "USER action={} result=success requestId={} userId={} status={} eventType={} userFound={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                HttpStatus.OK.value(),
                valueOrDefault(eventType),
                userFound,
                durationMs(startNanos)
        );
    }

    private void logUserFailure(
            String action,
            UUID userId,
            TossWebhookEventType eventType,
            ResponseStatusException exception,
            long startNanos
    ) {
        if (exception.getStatusCode().is5xxServerError()) {
            log.error(
                    "USER action={} result=fail requestId={} userId={} status={} eventType={} durationMs={}",
                    action,
                    RequestLogContext.currentRequestIdOrDefault(),
                    valueOrDefault(userId),
                    exception.getStatusCode().value(),
                    valueOrDefault(eventType),
                    durationMs(startNanos),
                    exception
            );
            return;
        }

        log.warn(
                "USER action={} result=fail requestId={} userId={} status={} eventType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                exception.getStatusCode().value(),
                valueOrDefault(eventType),
                durationMs(startNanos)
        );
    }

    private void logUnexpectedUserFailure(
            String action,
            UUID userId,
            TossWebhookEventType eventType,
            RuntimeException exception,
            long startNanos
    ) {
        log.error(
                "USER action={} result=fail requestId={} userId={} status={} eventType={} durationMs={}",
                action,
                RequestLogContext.currentRequestIdOrDefault(),
                valueOrDefault(userId),
                "unexpected",
                valueOrDefault(eventType),
                durationMs(startNanos),
                exception
        );
    }

    private String resolveWebhookAction(TossWebhookEventType eventType) {
        if (eventType == TossWebhookEventType.UNLINK) {
            return "tossUnlinkWebhook";
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
