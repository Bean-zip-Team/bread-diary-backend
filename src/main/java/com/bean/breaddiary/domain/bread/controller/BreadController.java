package com.bean.breaddiary.domain.bread.controller;

import com.bean.breaddiary.domain.bread.dto.response.BreadAutocompleteResponse;
import com.bean.breaddiary.domain.bread.service.BreadComplexService;
import com.bean.breaddiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
            summary = "빵 카탈로그 자동완성",
            description = "빵 기록 시 사용할 빵 이름 자동완성 목록을 조회합니다. q가 없으면 인기순 기본 목록을 반환합니다."
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
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId
    ) {
        BreadAutocompleteResponse response = breadComplexService.autocompleteBreads(query, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
