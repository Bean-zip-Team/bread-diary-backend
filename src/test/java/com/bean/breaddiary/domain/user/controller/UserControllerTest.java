package com.bean.breaddiary.domain.user.controller;

import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserStatsResponse;
import com.bean.breaddiary.domain.user.service.UserService;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private static final UUID AUTHENTICATED_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void getCurrentUserProfileReturnsSnakeCaseResponse() throws Exception {
        UserMeResponse response = new UserMeResponse(
                AUTHENTICATED_USER_ID,
                "빵순이",
                "bread@toss.im",
                "https://cdn.bread-diary.app/profiles/me.webp",
                "오늘도 빵을 먹습니다.",
                new UserStatsResponse(42L, 18L, 4.2),
                LocalDateTime.of(2026, 4, 1, 0, 0)
        );

        when(userService.getCurrentUserProfile(AUTHENTICATED_USER_ID))
                .thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(AUTHENTICATED_USER_ID.toString()))
                .andExpect(jsonPath("$.data.nickname").value("빵순이"))
                .andExpect(jsonPath("$.data.email").value("bread@toss.im"))
                .andExpect(jsonPath("$.data.profile_image_url").value("https://cdn.bread-diary.app/profiles/me.webp"))
                .andExpect(jsonPath("$.data.bio").value("오늘도 빵을 먹습니다."))
                .andExpect(jsonPath("$.data.stats.total_records").value(42))
                .andExpect(jsonPath("$.data.stats.unique_shops").value(18))
                .andExpect(jsonPath("$.data.stats.avg_rating").value(4.2))
                .andExpect(jsonPath("$.data.created_at").value("2026-04-01T00:00:00"))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.stats.totalRecords").doesNotExist());

        verify(userService).getCurrentUserProfile(AUTHENTICATED_USER_ID);
    }

    private JsonMapper snakeCaseObjectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
}
