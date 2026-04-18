package com.bean.breaddiary.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토스 로그인 요청")
public class TossLoginRequest {

    @NotBlank
    @Schema(description = "토스 authorization code", example = "auth-code-example")
    private String authorizationCode;

    @NotBlank
    @Schema(description = "토스 referrer", example = "DEFAULT")
    private String referrer;
}
