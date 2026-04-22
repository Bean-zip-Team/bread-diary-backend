package com.bean.breaddiary.domain.event.client;

import com.bean.breaddiary.domain.event.service.EventName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AmplitudeEventClient {

    private static final String AMPLITUDE_ENDPOINT = "https://api2.amplitude.com/2/httpapi";

    private final RestClient restClient;
    private final String apiKey;

    public AmplitudeEventClient(
            RestClient.Builder restClientBuilder,
            @Value("${AMPLITUDE_API_KEY:}") String apiKey
    ) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    public void forward(
            EventName eventName,
            Map<String, Object> properties,
            OffsetDateTime occurredAt,
            String anonymousId,
            UUID userId,
            String requestId
    ) {
        long startNanos = System.nanoTime();

        if (!StringUtils.hasText(apiKey)) {
            log.info(
                    "EVENT action=eventForward result=skipped requestId={} eventName={} status={} durationMs={}",
                    requestId,
                    eventName.value(),
                    "missingApiKey",
                    durationMs(startNanos)
            );
            return;
        }

        try {
            restClient.post()
                    .uri(AMPLITUDE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AmplitudeRequest(
                            apiKey,
                            List.of(new AmplitudeEvent(
                                    eventName.value(),
                                    userId == null ? null : userId.toString(),
                                    anonymousId,
                                    occurredAt.toInstant().toEpochMilli(),
                                    properties
                            ))
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "EVENT action=eventForward result=success requestId={} eventName={} status={} durationMs={}",
                    requestId,
                    eventName.value(),
                    HttpStatus.OK.value(),
                    durationMs(startNanos)
            );
        } catch (RestClientResponseException exception) {
            logForwardFailure(eventName, requestId, exception.getStatusCode().value(), exception, startNanos);
        } catch (RuntimeException exception) {
            logForwardFailure(eventName, requestId, "unexpected", exception, startNanos);
        }
    }

    private void logForwardFailure(
            EventName eventName,
            String requestId,
            Object status,
            RuntimeException exception,
            long startNanos
    ) {
        int statusCode = status instanceof Integer value ? value : HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusCode >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(
                    "EVENT action=eventForward result=fail requestId={} eventName={} status={} exceptionType={} durationMs={}",
                    requestId,
                    eventName.value(),
                    status,
                    exception.getClass().getSimpleName(),
                    durationMs(startNanos)
            );
            return;
        }

        log.warn(
                "EVENT action=eventForward result=fail requestId={} eventName={} status={} exceptionType={} durationMs={}",
                requestId,
                eventName.value(),
                status,
                exception.getClass().getSimpleName(),
                durationMs(startNanos)
        );
    }

    private long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private record AmplitudeRequest(
            @JsonProperty("api_key")
            String apiKey,
            @JsonProperty("events")
            List<AmplitudeEvent> events
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record AmplitudeEvent(
            @JsonProperty("event_type")
            String eventType,
            @JsonProperty("user_id")
            String userId,
            @JsonProperty("device_id")
            String deviceId,
            @JsonProperty("time")
            long time,
            @JsonProperty("event_properties")
            Map<String, Object> eventProperties
    ) {
    }
}
