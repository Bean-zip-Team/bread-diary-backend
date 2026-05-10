package com.bean.breaddiary.domain.onboarding.controller;

import com.bean.breaddiary.domain.onboarding.dto.response.OnboardingBreadListResponse;
import com.bean.breaddiary.domain.onboarding.service.OnboardingComplexService;
import com.bean.breaddiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "온보딩 API", description = "온보딩에서 선택 가능한 고정 빵 목록을 조회하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingComplexService onboardingComplexService;

    @Operation(
            summary = "온보딩 빵 목록 조회",
            description = "온보딩에서 선택 가능한 고정 30개 빵 목록을 순서대로 조회합니다. 각 항목의 imageUrl은 시스템 카탈로그 원본 이미지를 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "온보딩 빵 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = OnboardingBreadListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "온보딩 빵 목록 구성 오류")
    })
    @GetMapping("/breads")
    public ResponseEntity<ApiResponse<OnboardingBreadListResponse>> getOnboardingBreads() {
        return ResponseEntity.ok(ApiResponse.success(onboardingComplexService.getOnboardingBreads()));
    }
}
