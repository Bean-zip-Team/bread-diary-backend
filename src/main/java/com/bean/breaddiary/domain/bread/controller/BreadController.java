package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadCatalogListResponse;
import com.bean.breaddiary.domain.bread.dto.response.BreadProfileResponse;
import com.bean.breaddiary.domain.bread.service.BreadComplexService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "빵 카탈로그 API", description = "빵 카탈로그와 자동완성을 관리하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/breads")
public class BreadController {

    private final BreadComplexService breadComplexService;

    @Operation(
            summary = "빵 도감 목록 조회",
            description = "전체 빵 카탈로그를 조회합니다. Authorization 헤더가 있으면 현재 사용자 기준 수집 상태를 포함하고, 없으면 비로그인 카탈로그 정보만 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 도감 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadCatalogListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BreadCatalogListResponse>> getBreadCatalog(
            @Parameter(description = "정렬 기준: sticker_number, latest, rating", example = "sticker_number")
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "수집 상태 필터: all, collected, uncollected", example = "all")
            @RequestParam(value = "filter", required = false) String filter,
            @Parameter(description = "빵 종류 필터", example = "PASTRY")
            @RequestParam(value = "bread_type", required = false) String breadType,
            @Parameter(description = "빵 이름 검색어", example = "크루아상")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "페이지네이션 커서", example = "6")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "페이지당 개수. 기본 20, 최대 50", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        UUID userId = AuthRequestAttributes.getOptionalUserId(request);
        BreadCatalogListResponse response = breadComplexService.getBreadCatalog(
                sort,
                filter,
                breadType,
                search,
                cursor,
                limit,
                userId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "빵 프로필 조회",
            description = "카탈로그 빵의 마스터 정보를 조회합니다. Authorization 헤더가 있으면 현재 사용자 기준 기록 통계와 기록 목록을 포함합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadProfileResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 카탈로그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/catalog/{breadId}")
    public ResponseEntity<ApiResponse<BreadProfileResponse>> getBreadProfile(
            @Parameter(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
            @PathVariable UUID breadId,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        UUID userId = AuthRequestAttributes.getOptionalUserId(request);
        BreadProfileResponse response = breadComplexService.getBreadProfile(
                breadId,
                userId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "빵 카탈로그 자동완성",
            description = "빵 기록 시 사용할 빵 이름 자동완성 목록을 조회합니다. q가 없으면 인기순 기본 목록을 반환하며, Authorization 헤더가 있으면 현재 사용자 기준 기록 수를 포함합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "자동완성 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadAutocompleteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<BreadAutocompleteResponse>> autocompleteBreads(
            @Parameter(description = "검색어. 미입력 시 인기순 빵 목록을 반환합니다.", example = "크루")
            @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "페이지네이션 커서. 검색어가 있으면 마지막 sticker_number, 없으면 {record_count}_{sticker_number} 형식", example = "6")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "페이지당 개수. 기본 20, 최대 50", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        UUID userId = AuthRequestAttributes.getOptionalUserId(request);
        BreadAutocompleteResponse response = breadComplexService.autocompleteBreads(query, cursor, limit, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
