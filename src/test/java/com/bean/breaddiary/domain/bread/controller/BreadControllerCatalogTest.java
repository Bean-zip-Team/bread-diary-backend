package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.bread.service.BreadComplexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadControllerCatalogTest {

    private BreadComplexService breadComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        breadComplexService = mock(BreadComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BreadController(breadComplexService))
                .build();
    }

    @Test
    void getBreadCatalogReturnsSpecResponseWithSnakeCaseFields() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        BreadCatalogListResponse response = new BreadCatalogListResponse(
                List.of(new BreadCatalogItemResponse(
                        breadId,
                        6,
                        "크루아상",
                        BreadType.PASTRY,
                        "https://cdn.bread-diary.app/breads/croissant.webp",
                        true,
                        5L,
                        4.8,
                        "https://cdn.bread-diary.app/bread-photos/record_thumb.webp",
                        LocalDate.of(2026, 3, 14)
                )),
                "6",
                true,
                42L
        );

        when(breadComplexService.getBreadCatalog(
                "sticker_number",
                "collected",
                BreadType.PASTRY,
                "크루",
                null,
                20,
                userId
        )).thenReturn(response);

        mockMvc.perform(get("/breads")
                        .param("sort", "sticker_number")
                        .param("filter", "collected")
                        .param("bread_type", "PASTRY")
                        .param("search", "크루")
                        .param("limit", "20")
                        .header("X-USER-ID", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.items[0].sticker_number").value(6))
                .andExpect(jsonPath("$.data.items[0].name").value("크루아상"))
                .andExpect(jsonPath("$.data.items[0].bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.items[0].image_url").value("https://cdn.bread-diary.app/breads/croissant.webp"))
                .andExpect(jsonPath("$.data.items[0].is_collected").value(true))
                .andExpect(jsonPath("$.data.items[0].eat_count").value(5))
                .andExpect(jsonPath("$.data.items[0].avg_rating").value(4.8))
                .andExpect(jsonPath("$.data.items[0].latest_photo_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"))
                .andExpect(jsonPath("$.data.items[0].latest_eaten_date").value("2026-03-14"))
                .andExpect(jsonPath("$.data.next_cursor").value("6"))
                .andExpect(jsonPath("$.data.has_more").value(true))
                .andExpect(jsonPath("$.data.total_count").value(42))
                .andExpect(jsonPath("$.data.items[0].breadId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].isCollected").doesNotExist());

        verify(breadComplexService).getBreadCatalog(
                "sticker_number",
                "collected",
                BreadType.PASTRY,
                "크루",
                null,
                20,
                userId
        );
    }
}
