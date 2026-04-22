package com.bean.breaddiary.global.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void responseStatusExceptionReturnsCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/auth-required"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    void validationFailureReturnsCommonErrorResponse() throws Exception {
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("이름은 필수입니다."));
    }

    @Test
    void unsupportedMediaTypeReturnsCommonErrorResponse() throws Exception {
        mockMvc.perform(post("/json-only")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("name=bread"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void validationFailureExceptionReturnsValidationFailedCode() throws Exception {
        mockMvc.perform(get("/validation-failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("이벤트 속성에 허용되지 않는 항목이 포함되어 있습니다."));
    }

    @RestController
    private static class TestController {

        @GetMapping("/auth-required")
        void authRequired() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @PostMapping(value = "/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
        void jsonOnly(@RequestBody TestRequest request) {
        }

        @GetMapping("/validation-failure")
        void validationFailure() {
            throw new ValidationFailureException("이벤트 속성에 허용되지 않는 항목이 포함되어 있습니다.");
        }
    }

    private record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }
}
