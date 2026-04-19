package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadProfileRecordResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileStatsResponse;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.bread.service.BreadComplexService;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadControllerProfileTest {

    private BreadComplexService breadComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        breadComplexService = mock(BreadComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BreadController(breadComplexService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void getBreadProfileReturnsSpecResponseWithSnakeCaseFields() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        UUID recordId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        BreadProfileResponse response = new BreadProfileResponse(
                breadId,
                6,
                "크루아상",
                BreadType.PASTRY,
                "페이스트리",
                "https://cdn.bread-diary.app/breads/croissant.webp",
                new BreadProfileStatsResponse(
                        5L,
                        4.8,
                        LocalDateTime.of(2026, 3, 1, 9, 30)
                ),
                List.of(new BreadProfileRecordResponse(
                        recordId,
                        "https://cdn.bread-diary.app/bread-photos/record_thumb.webp",
                        5,
                        "르뺑블루 성수점",
                        LocalDate.of(2026, 3, 12),
                        LocalDateTime.of(2026, 3, 12, 9, 30)
                ))
        );

        when(breadComplexService.getBreadProfile(breadId, userId)).thenReturn(response);

        mockMvc.perform(get("/breads/catalog/{breadId}", breadId)
                        .requestAttr(AuthRequestAttributes.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.sticker_number").value(6))
                .andExpect(jsonPath("$.data.name").value("크루아상"))
                .andExpect(jsonPath("$.data.bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.bread_type_label").value("페이스트리"))
                .andExpect(jsonPath("$.data.image_url").value("https://cdn.bread-diary.app/breads/croissant.webp"))
                .andExpect(jsonPath("$.data.stats.eat_count").value(5))
                .andExpect(jsonPath("$.data.stats.avg_rating").value(4.8))
                .andExpect(jsonPath("$.data.stats.first_recorded_at").value("2026-03-01T09:30:00"))
                .andExpect(jsonPath("$.data.records[0].id").value(recordId.toString()))
                .andExpect(jsonPath("$.data.records[0].photo_thumbnail_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"))
                .andExpect(jsonPath("$.data.records[0].rating").value(5))
                .andExpect(jsonPath("$.data.records[0].shop_name").value("르뺑블루 성수점"))
                .andExpect(jsonPath("$.data.records[0].eaten_date").value("2026-03-12"))
                .andExpect(jsonPath("$.data.records[0].created_at").value("2026-03-12T09:30:00"))
                .andExpect(jsonPath("$.data.breadId").doesNotExist())
                .andExpect(jsonPath("$.data.stats.eatCount").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].photoThumbnailUrl").doesNotExist());

        verify(breadComplexService).getBreadProfile(breadId, userId);
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
