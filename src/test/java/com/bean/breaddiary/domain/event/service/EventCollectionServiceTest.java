package com.bean.breaddiary.domain.event.service;

import com.bean.breaddiary.domain.event.client.AmplitudeEventClient;
import com.bean.breaddiary.domain.event.dto.request.EventCollectRequest;
import com.bean.breaddiary.domain.event.dto.response.EventCollectResponse;
import com.bean.breaddiary.global.common.ValidationFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventCollectionServiceTest {

    private AmplitudeEventClient amplitudeEventClient;
    private EventCollectionService eventCollectionService;

    @BeforeEach
    void setUp() {
        amplitudeEventClient = mock(AmplitudeEventClient.class);
        eventCollectionService = new EventCollectionService(
                new EventPropertySanitizer(),
                amplitudeEventClient
        );
    }

    @Test
    void collectRejectsAnonymousEventWithoutAnonymousId() {
        EventCollectRequest request = new EventCollectRequest(
                "screen_viewed",
                Map.of("screen", "home"),
                null,
                null
        );

        assertThrows(
                ValidationFailureException.class,
                () -> eventCollectionService.collect(request, null, null)
        );
    }

    @Test
    void collectAllowsAuthenticatedEventWithoutAnonymousId() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
        EventCollectRequest request = new EventCollectRequest(
                "logout_clicked",
                null,
                null,
                null
        );

        eventCollectionService.collect(request, userId, sessionId);

        verify(amplitudeEventClient).forward(
                eq(EventName.LOGOUT_CLICKED),
                eq(Map.of()),
                any(OffsetDateTime.class),
                eq(null),
                eq(userId),
                any(String.class)
        );
    }

    @Test
    void collectParsesOccurredAtAndSanitizesProperties() {
        EventCollectRequest request = new EventCollectRequest(
                "screen_viewed",
                Map.of("screen", "home"),
                "2026-04-22T12:00:00Z",
                "anon-123"
        );

        eventCollectionService.collect(request, null, null);

        ArgumentCaptor<OffsetDateTime> occurredAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(amplitudeEventClient).forward(
                eq(EventName.SCREEN_VIEWED),
                eq(Map.of("screen", "home")),
                occurredAtCaptor.capture(),
                eq("anon-123"),
                eq(null),
                any(String.class)
        );
        assertEquals(OffsetDateTime.parse("2026-04-22T12:00:00Z"), occurredAtCaptor.getValue());
    }

    @Test
    void collectRejectsInvalidOccurredAt() {
        EventCollectRequest request = new EventCollectRequest(
                "screen_viewed",
                null,
                "invalid-date-time",
                "anon-123"
        );

        assertThrows(
                ValidationFailureException.class,
                () -> eventCollectionService.collect(request, null, null)
        );
    }

    @Test
    void collectDoesNotPropagateAmplitudeFailure() {
        EventCollectRequest request = new EventCollectRequest(
                "screen_viewed",
                Map.of("screen", "home"),
                null,
                "anon-123"
        );

        doThrow(new RuntimeException("external-secret-response-body"))
                .when(amplitudeEventClient)
                .forward(any(EventName.class), anyMap(), any(OffsetDateTime.class), any(), any(), any(String.class));

        EventCollectResponse response = assertDoesNotThrow(() -> eventCollectionService.collect(request, null, null));

        assertNotNull(response);
    }

    @Test
    void collectRejectsUnknownEventName() {
        EventCollectRequest request = new EventCollectRequest(
                "unknown_event",
                null,
                null,
                "anon-123"
        );

        assertThrows(
                ValidationFailureException.class,
                () -> eventCollectionService.collect(request, null, null)
        );
    }
}
