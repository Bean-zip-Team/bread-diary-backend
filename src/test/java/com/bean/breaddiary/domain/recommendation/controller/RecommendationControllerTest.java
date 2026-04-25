package com.bean.breaddiary.domain.recommendation.controller;

import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayItemResponse;
import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayResponse;
import com.bean.breaddiary.domain.recommendation.service.RecommendationComplexService;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerTest {

    private RecommendationComplexService recommendationComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        recommendationComplexService = mock(RecommendationComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecommendationController(recommendationComplexService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(camelCaseObjectMapper()))
                .build();
    }

    @Test
    void getTodayRecommendationsReturnsCamelCaseResponse() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        BreadTodayResponse response = new BreadTodayResponse(
                LocalDate.of(2026, 4, 26),
                List.of(new BreadTodayItemResponse(
                        breadId,
                        "소금빵",
                        "PASTRY",
                        "https://cdn.bread-diary.app/breads/salt_placeholder.webp",
                        128L,
                        false
                ))
        );

        when(recommendationComplexService.getTodayRecommendations(userId)).thenReturn(response);

        mockMvc.perform(get("/breads/today")
                        .requestAttr(AuthRequestAttributes.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2026-04-26"))
                .andExpect(jsonPath("$.data.breads[0].id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.breads[0].name").value("소금빵"))
                .andExpect(jsonPath("$.data.breads[0].type").value("PASTRY"))
                .andExpect(jsonPath("$.data.breads[0].imageUrl").value("https://cdn.bread-diary.app/breads/salt_placeholder.webp"))
                .andExpect(jsonPath("$.data.breads[0].totalRecordCount").value(128))
                .andExpect(jsonPath("$.data.breads[0].isCollected").value(false))
                .andExpect(jsonPath("$.data.breads[0].image_url").doesNotExist())
                .andExpect(jsonPath("$.data.breads[0].total_record_count").doesNotExist())
                .andExpect(jsonPath("$.data.breads[0].is_collected").doesNotExist());

        verify(recommendationComplexService).getTodayRecommendations(userId);
    }

    @Test
    void getTodayRecommendationsAllowsAnonymousRequest() throws Exception {
        when(recommendationComplexService.getTodayRecommendations(null))
                .thenReturn(new BreadTodayResponse(LocalDate.of(2026, 4, 26), List.of()));

        mockMvc.perform(get("/breads/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.breads").isArray());

        verify(recommendationComplexService).getTodayRecommendations(null);
    }

    private JsonMapper camelCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
