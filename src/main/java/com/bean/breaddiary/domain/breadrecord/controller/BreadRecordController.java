package com.bean.breaddiary.domain.breadrecord.controller;

import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.UpdateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDeleteResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordComplexService;
import com.bean.breaddiary.global.common.ApiResponse;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "빵 기록 API", description = "빵 기록을 관리하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/breads")
public class BreadRecordController {

    private final BreadRecordComplexService breadRecordComplexService;

    @Operation(
            summary = "개별 빵 기록 상세 조회",
            description = "빵 기록 ID 기준으로 개별 기록 상세를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<BreadRecordDetailResponse>> getBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        BreadRecordDetailResponse response = breadRecordComplexService.getBreadRecord(
                resolveUserId(request),
                recordId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "기존 빵 기록 생성",
            description = "카탈로그에 있는 빵으로 기록을 생성합니다. multipart/form-data 필드는 camelCase를 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "빵 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 카탈로그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordCreateResponse>> createBreadRecord(
            @Parameter(hidden = true) HttpServletRequest request,
            @Valid @ModelAttribute CreateBreadRecordRequest createBreadRecordRequest
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createBreadRecord(
                resolveUserId(request),
                createBreadRecordRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "신규 빵 추가 및 기록 생성",
            description = "카탈로그에 없는 신규 빵을 추가하고 첫 기록을 생성합니다. multipart/form-data 필드는 camelCase를 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "신규 빵 및 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 빵 이름"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(path = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordCreateResponse>> createNewBreadRecord(
            @Parameter(hidden = true) HttpServletRequest request,
            @Valid @ModelAttribute CreateNewBreadRecordRequest createNewBreadRecordRequest
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createNewBreadRecord(
                resolveUserId(request),
                createNewBreadRecordRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "개별 빵 기록 수정",
            description = "빵 기록 ID 기준으로 변경한 필드만 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 수정 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping(path = "/{recordId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordDetailResponse>> updateBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(hidden = true) HttpServletRequest request,
            @Valid @ModelAttribute UpdateBreadRecordRequest updateBreadRecordRequest
    ) {
        BreadRecordDetailResponse response = breadRecordComplexService.updateBreadRecord(
                resolveUserId(request),
                recordId,
                updateBreadRecordRequest
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "개별 빵 기록 삭제",
            description = "빵 기록 ID 기준으로 기록을 삭제하고 스티커 제거 여부를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 삭제 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDeleteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<BreadRecordDeleteResponse>> deleteBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        BreadRecordDeleteResponse response = breadRecordComplexService.deleteBreadRecord(
                resolveUserId(request),
                recordId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID resolveUserId(HttpServletRequest request) {
        return AuthRequestAttributes.getRequiredUserId(request);
    }
}
