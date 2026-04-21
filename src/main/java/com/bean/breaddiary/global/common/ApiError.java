package com.bean.breaddiary.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 API 에러")
public class ApiError {

    @Schema(description = "에러 코드", example = "UNAUTHORIZED")
    private String code;

    @Schema(description = "에러 메시지", example = "인증이 필요합니다.")
    private String message;
}
