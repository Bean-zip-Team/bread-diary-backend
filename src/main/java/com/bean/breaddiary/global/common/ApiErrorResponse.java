package com.bean.breaddiary.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 API 에러 응답")
public class ApiErrorResponse {

    @Schema(description = "요청 성공 여부", example = "false")
    private boolean success;

    @Schema(description = "에러 정보")
    private ApiError error;

    public static ApiErrorResponse failure(String code, String message) {
        return new ApiErrorResponse(false, new ApiError(code, message));
    }
}
