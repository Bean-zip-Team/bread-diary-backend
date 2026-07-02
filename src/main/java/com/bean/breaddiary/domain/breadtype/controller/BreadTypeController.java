package com.bean.breaddiary.domain.breadtype.controller;

import com.bean.breaddiary.domain.breadtype.dto.response.BreadTypeListResponse;
import com.bean.breaddiary.domain.breadtype.service.BreadTypeService;
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

@Tag(name = "빵 종류 API", description = "빵 종류 목록을 조회하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/bread-types")
public class BreadTypeController {

    private final BreadTypeService breadTypeService;

    @Operation(
            summary = "빵 종류 목록 조회",
            description = "신규 빵 기록 화면에서 사용할 빵 종류 코드와 한글 라벨 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 종류 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadTypeListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BreadTypeListResponse>> getBreadTypes() {
        BreadTypeListResponse response = breadTypeService.getBreadTypes();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
