package com.bean.breaddiary.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증 토큰 응답")
public class AuthTokenResponse {

    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9.access.signature")
    private String accessToken;

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.signature")
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access Token 만료 시각", example = "2026-04-20T10:00:00")
    private LocalDateTime accessTokenExpiresAt;

    @Schema(description = "Refresh Token 만료 시각", example = "2026-05-19T10:00:00")
    private LocalDateTime refreshTokenExpiresAt;

    @Schema(description = "신규 가입 여부", example = "false")
    private boolean newUser;
}
