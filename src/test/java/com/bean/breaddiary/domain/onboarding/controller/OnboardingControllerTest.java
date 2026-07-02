package com.bean.breaddiary.domain.onboarding.controller;

import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadItemResponse;
import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadListResponse;
import com.bean.breaddiary.domain.onboarding.service.OnboardingComplexService;
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

class OnboardingControllerTest {

    private OnboardingComplexService onboardingComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        onboardingComplexService = mock(OnboardingComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OnboardingController(onboardingComplexService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void getOnboardingBreadsReturnsSpecResponseWithSnakeCaseFields() throws Exception {
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        OnboardingBreadListResponse response = new OnboardingBreadListResponse(
                List.of(new OnboardingBreadItemResponse(
                        breadId,
                        "생식빵",
                        41,
                        "BREAD",
                        "https://du4zizlgiw14n.cloudfront.net/images/webp/041_%EC%83%9D%EC%8B%9D%EB%B9%B5.webp"
                ))
        );
        when(onboardingComplexService.getOnboardingBreads()).thenReturn(response);

        mockMvc.perform(get("/onboarding/breads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("생식빵"))
                .andExpect(jsonPath("$.data.items[0].sticker_number").value(41))
                .andExpect(jsonPath("$.data.items[0].bread_type").value("BREAD"))
                .andExpect(jsonPath("$.data.items[0].image_url").value("https://du4zizlgiw14n.cloudfront.net/images/webp/041_%EC%83%9D%EC%8B%9D%EB%B9%B5.webp"))
                .andExpect(jsonPath("$.data.items[0].breadId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].imageUrl").doesNotExist());

        verify(onboardingComplexService).getOnboardingBreads();
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
