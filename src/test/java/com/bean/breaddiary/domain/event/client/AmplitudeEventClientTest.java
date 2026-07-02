package com.bean.breaddiary.domain.event.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bean.breaddiary.domain.event.service.EventName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AmplitudeEventClientTest {

    private static final String AMPLITUDE_URL = "https://api2.amplitude.com/2/httpapi";

    private MockRestServiceServer mockServer;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AmplitudeEventClient.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void forwardDoesNotCallAmplitudeWhenApiKeyIsMissing() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        AmplitudeEventClient amplitudeEventClient = new AmplitudeEventClient(restClientBuilder, "");

        amplitudeEventClient.forward(
                EventName.SCREEN_VIEWED,
                Map.of("screen", "home"),
                OffsetDateTime.parse("2026-04-22T12:00:00Z"),
                "anon-123",
                null,
                "request-id"
        );

        mockServer.verify();
        assertTrue(formattedLogs().contains("status=missingApiKey"));
    }

    @Test
    void forwardSendsAuthenticatedEventWithUserIdAndDeviceId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        AmplitudeEventClient amplitudeEventClient = new AmplitudeEventClient(restClientBuilder, "api-key");
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");

        mockServer.expect(requestTo(AMPLITUDE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "api_key": "api-key",
                          "events": [
                            {
                              "event_type": "screen_viewed",
                              "user_id": "550e8400-e29b-41d4-a716-446655440100",
                              "device_id": "anon-123",
                              "time": 1776859200000,
                              "event_properties": {
                                "screen": "home"
                              }
                            }
                          ]
                        }
                        """))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        amplitudeEventClient.forward(
                EventName.SCREEN_VIEWED,
                Map.of("screen", "home"),
                OffsetDateTime.parse("2026-04-22T12:00:00Z"),
                "anon-123",
                userId,
                "request-id"
        );

        mockServer.verify();
    }

    @Test
    void forwardSendsAnonymousEventWithDeviceId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        AmplitudeEventClient amplitudeEventClient = new AmplitudeEventClient(restClientBuilder, "api-key");

        mockServer.expect(requestTo(AMPLITUDE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "api_key": "api-key",
                          "events": [
                            {
                              "event_type": "app_opened",
                              "device_id": "anon-123",
                              "time": 1776859200000,
                              "event_properties": {}
                            }
                          ]
                        }
                        """))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        amplitudeEventClient.forward(
                EventName.APP_OPENED,
                Map.of(),
                OffsetDateTime.parse("2026-04-22T12:00:00Z"),
                "anon-123",
                null,
                "request-id"
        );

        mockServer.verify();
    }

    @Test
    void forwardDoesNotLogExternalResponseBodyWhenAmplitudeFails() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        AmplitudeEventClient amplitudeEventClient = new AmplitudeEventClient(restClientBuilder, "api-key");
        String responseBody = "external-secret-response-body";

        mockServer.expect(requestTo(AMPLITUDE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(responseBody));

        amplitudeEventClient.forward(
                EventName.SCREEN_VIEWED,
                Map.of("screen", "home"),
                OffsetDateTime.parse("2026-04-22T12:00:00Z"),
                "anon-123",
                null,
                "request-id"
        );

        mockServer.verify();
        String logs = formattedLogs();
        assertTrue(logs.contains("action=eventForward"));
        assertTrue(logs.contains("eventName=screen_viewed"));
        assertTrue(logs.contains("status=500"));
        assertTrue(logs.contains("exceptionType="));
        assertFalse(logs.contains(responseBody));
        assertTrue(listAppender.list.stream().allMatch(event -> event.getThrowableProxy() == null));
    }

    private String formattedLogs() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }
}
