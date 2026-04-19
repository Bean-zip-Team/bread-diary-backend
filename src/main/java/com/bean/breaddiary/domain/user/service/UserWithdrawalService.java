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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
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
        User user = userService.getUserById(userId);

        if (StringUtils.hasText(user.getTossUserKey())) {
            tossAuthClient.unlinkByUserKey(user.getTossUserKey());
        }

        hardDeleteUserData(user);
        return new UserWithdrawalResponse(true);
    }

    @Transactional
    public TossWebhookResponse handleTossWebhook(
            String requestWebhookSecret,
            TossWebhookRequest request
    ) {
        validateWebhookSecret(requestWebhookSecret);

        Optional<User> user = userRepository.findByTossUserKey(normalizeText(request.getUserKey()));
        if (user.isEmpty()) {
            return new TossWebhookResponse(true, request.getEventType());
        }

        if (request.getEventType() == TossWebhookEventType.UNLINK) {
            clearSessionsOnly(user.get().getId());
            return new TossWebhookResponse(true, request.getEventType());
        }

        hardDeleteUserData(user.get());
        return new TossWebhookResponse(true, request.getEventType());
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

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
