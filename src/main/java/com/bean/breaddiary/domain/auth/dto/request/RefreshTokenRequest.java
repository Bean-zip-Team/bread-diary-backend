package com.bean.breaddiary.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
@Schema(description = "리프레시 토큰 요청")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.refresh.signature")
    @JsonAlias("refreshToken")
    private String refreshToken;
}
