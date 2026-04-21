package com.bean.breaddiary.domain.auth.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TossAuthClientTest {

    private static final String BASE_URL = "https://toss.example";
    private static final String GENERATE_TOKEN_URL =
            BASE_URL + "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";

    private TossAuthClient tossAuthClient;
    private MockRestServiceServer mockServer;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        tossAuthClient = new TossAuthClient(
                restClientBuilder,
                new ObjectMapper(),
                BASE_URL,
                "unlink-access-token"
        );

        logger = (Logger) LoggerFactory.getLogger(TossAuthClient.class);
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
    void exchangeAuthorizationCodeDoesNotLogExternalResponseBodyWhenTossReturnsServerError() {
        String secretResponseBody = "external-secret-response-body";
        String responseBody = """
                {
                  "error": "TOSS_DOWN",
                  "reason": "external-secret-response-body"
                }
                """;

        mockServer.expect(requestTo(GENERATE_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tossAuthClient.exchangeAuthorizationCode("authorization-code", "DEFAULT")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        mockServer.verify();

        String logs = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertTrue(logs.contains("action=tossExchange"));
        assertTrue(logs.contains("status=500"));
        assertTrue(logs.contains("errorCode=TOSS_DOWN"));
        assertTrue(logs.contains("exceptionType="));
        assertFalse(logs.contains(secretResponseBody));
        assertFalse(logs.contains(responseBody));
        assertTrue(listAppender.list.stream().allMatch(event -> event.getThrowableProxy() == null));
    }
}
