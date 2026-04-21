package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadControllerAutocompleteTest {

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
    void autocompleteBreadsReturnsSpecResponseWithSnakeCaseFields() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        BreadAutocompleteResponse response = new BreadAutocompleteResponse(List.of(
                new BreadAutocompleteItemResponse(
                        breadId,
                        "크루아상",
                        "PASTRY",
                        6,
                        "https://cdn.bread-diary.app/breads/croissant.webp",
                        5L
                )
        ));

        when(breadComplexService.autocompleteBreads("크루", userId)).thenReturn(response);

        mockMvc.perform(get("/breads/autocomplete")
                        .param("q", "크루")
                        .requestAttr(AuthRequestAttributes.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("크루아상"))
                .andExpect(jsonPath("$.data.items[0].bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.items[0].sticker_number").value(6))
                .andExpect(jsonPath("$.data.items[0].image_url").value("https://cdn.bread-diary.app/breads/croissant.webp"))
                .andExpect(jsonPath("$.data.items[0].eat_count").value(5))
                .andExpect(jsonPath("$.data.items[0].breadId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].breadType").doesNotExist());

        verify(breadComplexService).autocompleteBreads("크루", userId);
    }

    @Test
    void autocompleteBreadsAllowsAnonymousRequest() throws Exception {
        when(breadComplexService.autocompleteBreads(null, null))
                .thenReturn(new BreadAutocompleteResponse(List.of()));

        mockMvc.perform(get("/breads/autocomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        verify(breadComplexService).autocompleteBreads(null, null);
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
