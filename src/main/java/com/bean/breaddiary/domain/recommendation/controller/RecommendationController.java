package com.bean.breaddiary.domain.recommendation.controller;

import com.bean.breaddiary.domain.recommendation.dto.response.BreadTodayResponse;
import com.bean.breaddiary.domain.recommendation.service.RecommendationComplexService;
import com.bean.breaddiary.global.common.ApiResponse;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "오늘의 추천 빵 API", description = "오늘 기록해볼 추천 빵을 조회하는 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/breads")
public class RecommendationController {

    private final RecommendationComplexService recommendationComplexService;

    @Operation(
            summary = "오늘의 추천 빵 조회",
            description = "오늘의 추천 빵 5개를 조회합니다. Authorization 헤더가 있으면 현재 사용자 기준 수집 여부와 이미지 표현을 반영합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "오늘의 추천 빵 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadTodayResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "오늘의 추천 빵 목록을 생성하거나 조회하는 중 서버 오류가 발생했습니다."
            )
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<BreadTodayResponse>> getTodayRecommendations(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(recommendationComplexService.getTodayRecommendations(AuthRequestAttributes.getOptionalUserId(request))));
    }
}
