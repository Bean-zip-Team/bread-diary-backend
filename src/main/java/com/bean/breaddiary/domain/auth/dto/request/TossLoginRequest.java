package com.bean.breaddiary.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토스 로그인 요청")
public class TossLoginRequest {

    public TossLoginRequest(String authorizationCode, String referrer) {
        this.authorizationCode = authorizationCode;
        this.referrer = referrer;
    }

    @NotBlank
    @Schema(description = "토스 authorization code", example = "auth-code-example")
    @JsonAlias("authorizationCode")
    private String authorizationCode;

    @NotBlank
    @Schema(description = "토스 referrer", example = "DEFAULT")
    private String referrer;

    @ArraySchema(
            arraySchema = @Schema(
                    description = "온보딩에서 선택한 기존 카탈로그 빵 ID 목록. 구버전 클라이언트는 생략할 수 있습니다."
            ),
            schema = @Schema(
                    description = "카탈로그 빵 ID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
    )
    @JsonAlias("selectedBreadIds")
    @Valid
    private List<@NotNull UUID> selectedBreadIds;
}
