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
@Schema(description = "로그아웃 요청")
public class LogoutRequest {

    @NotBlank
    @Schema(description = "현재 세션의 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.refresh.signature")
    private String refreshToken;
}
