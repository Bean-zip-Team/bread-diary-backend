package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadTypeItemResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadTypeListResponse;
import com.bean.breaddiary.domain.bread.service.BreadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadTypeControllerTest {

    private BreadService breadService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        breadService = mock(BreadService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BreadTypeController(breadService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void getBreadTypesReturnsAllBreadTypes() throws Exception {
        BreadTypeListResponse response = new BreadTypeListResponse(List.of(
                new BreadTypeItemResponse("PASTRY", "페이스트리"),
                new BreadTypeItemResponse("BREAD", "식빵"),
                new BreadTypeItemResponse("DONUT", "도넛"),
                new BreadTypeItemResponse("CAKE", "케이크"),
                new BreadTypeItemResponse("BAGEL", "베이글"),
                new BreadTypeItemResponse("TART", "타르트"),
                new BreadTypeItemResponse("OTHER", "기타")
        ));

        when(breadService.getBreadTypes()).thenReturn(response);

        mockMvc.perform(get("/bread-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(7))
                .andExpect(jsonPath("$.data.items[0].code").value("PASTRY"))
                .andExpect(jsonPath("$.data.items[0].label").value("페이스트리"))
                .andExpect(jsonPath("$.data.items[1].code").value("BREAD"))
                .andExpect(jsonPath("$.data.items[1].label").value("식빵"))
                .andExpect(jsonPath("$.data.items[6].code").value("OTHER"))
                .andExpect(jsonPath("$.data.items[6].label").value("기타"));

        verify(breadService).getBreadTypes();
    }

    private ObjectMapper snakeCaseObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
