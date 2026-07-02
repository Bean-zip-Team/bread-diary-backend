package com.bean.breaddiary.domain.event.controller;

import com.bean.breaddiary.domain.event.dto.request.EventCollectRequest;
import com.bean.breaddiary.domain.event.dto.response.EventCollectResponse;
import com.bean.breaddiary.domain.event.service.EventCollectionService;
import com.bean.breaddiary.global.common.GlobalExceptionHandler;
import com.bean.breaddiary.global.common.ValidationFailureException;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private EventCollectionService eventCollectionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventCollectionService = mock(EventCollectionService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(eventCollectionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void collectEventReturnsAcceptedForAnonymousRequest() throws Exception {
        when(eventCollectionService.collect(any(EventCollectRequest.class), isNull(), isNull()))
                .thenReturn(new EventCollectResponse(true));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "screen_viewed",
                                  "anonymousId": "anon-123",
                                  "properties": {
                                    "screen": "home"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    void collectEventPassesAuthenticatedAttributesToService() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID sessionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        when(eventCollectionService.collect(any(EventCollectRequest.class), any(UUID.class), any(UUID.class)))
                .thenReturn(new EventCollectResponse(true));

        mockMvc.perform(post("/events")
                        .requestAttr(AuthRequestAttributes.USER_ID, userId)
                        .requestAttr(AuthRequestAttributes.SESSION_ID, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "logout_clicked"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true));

        ArgumentCaptor<EventCollectRequest> requestCaptor = ArgumentCaptor.forClass(EventCollectRequest.class);
        verify(eventCollectionService).collect(requestCaptor.capture(), eq(userId), eq(sessionId));
        assertEquals("logout_clicked", requestCaptor.getValue().getEventName());
    }

    @Test
    void collectEventReturnsValidationFailedWhenServiceRejectsRequest() throws Exception {
        when(eventCollectionService.collect(any(EventCollectRequest.class), isNull(), isNull()))
                .thenThrow(new ValidationFailureException("anonymousId가 필요합니다."));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "screen_viewed"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("anonymousId가 필요합니다."));
    }

    @Test
    void collectEventReturnsValidationFailedForBlankEventName() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "",
                                  "anonymousId": "anon-123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
