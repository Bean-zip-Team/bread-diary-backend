package com.bean.breaddiary.domain.event.service;

import com.bean.breaddiary.domain.event.client.AmplitudeEventClient;
import com.bean.breaddiary.domain.event.dto.request.EventCollectRequest;
import com.bean.breaddiary.domain.event.dto.response.EventCollectResponse;
import com.bean.breaddiary.global.common.ValidationFailureException;
import com.bean.breaddiary.global.logging.RequestLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCollectionService {

    private final EventPropertySanitizer eventPropertySanitizer;
    private final AmplitudeEventClient amplitudeEventClient;

    public EventCollectResponse collect(
            EventCollectRequest request,
            UUID userId,
            UUID sessionId
    ) {
        EventName eventName = EventName.fromValue(request.getEventName());
        String anonymousId = normalizeAnonymousId(request.getAnonymousId());
        validateIdentity(userId, anonymousId);

        Map<String, Object> properties = eventPropertySanitizer.sanitize(request.getProperties());
        OffsetDateTime occurredAt = resolveOccurredAt(request.getOccurredAt());
        String requestId = RequestLogContext.currentRequestIdOrDefault();

        try {
            amplitudeEventClient.forward(
                    eventName,
                    properties,
                    occurredAt,
                    anonymousId,
                    userId,
                    requestId
            );
        } catch (RuntimeException exception) {
            log.error(
                    "EVENT action=collect result=forwardFailure requestId={} eventName={} userId={} sessionId={} exceptionType={}",
                    requestId,
                    eventName.value(),
                    valueOrDefault(userId),
                    valueOrDefault(sessionId),
                    exception.getClass().getSimpleName()
            );
        }

        return new EventCollectResponse(true);
    }

    private void validateIdentity(UUID userId, String anonymousId) {
        if (userId == null && !StringUtils.hasText(anonymousId)) {
            throw new ValidationFailureException("anonymousId가 필요합니다.");
        }
    }

    private String normalizeAnonymousId(String anonymousId) {
        return StringUtils.hasText(anonymousId) ? anonymousId.trim() : null;
    }

    private OffsetDateTime resolveOccurredAt(String occurredAt) {
        if (!StringUtils.hasText(occurredAt)) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }

        try {
            return OffsetDateTime.parse(occurredAt.trim());
        } catch (DateTimeParseException exception) {
            throw new ValidationFailureException("occurredAt 형식이 올바르지 않습니다.");
        }
    }

    private String valueOrDefault(Object value) {
        return value == null ? "-" : value.toString();
    }
}
