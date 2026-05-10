package com.bean.breaddiary.domain.onboarding.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "온보딩 빵 목록 응답")
public class OnboardingBreadListResponse {

    @ArraySchema(
            schema = @Schema(implementation = OnboardingBreadItemResponse.class),
            arraySchema = @Schema(description = "온보딩에서 선택 가능한 고정 30개 빵 목록")
    )
    private List<OnboardingBreadItemResponse> items;
}
