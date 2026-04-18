package com.bean.breaddiary.domain.breadrecord.controller;

import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.UpdateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDeleteResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordComplexService;
import com.bean.breaddiary.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * packageName    : com.bean.breaddiary.domain.breadrecord.controller<br>
 * fileName       : BreadRecordController.java<br>
 * description    : BreadRecord entity 요청을 처리하는 controller 클래스입니다.<br>
 * ===========================================================<br>
 * DATE              AUTHOR             NOTE<br>
 * -----------------------------------------------------------<br>
 * 26.04.16          haelim             최초 생성<br>
 */
@Tag(name = "빵 기록 API", description = "빵 기록을 관리하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/breads")
public class BreadRecordController {

    private static final UUID DUMMY_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final BreadRecordComplexService breadRecordComplexService;

    @Operation(
            summary = "개별 빵 기록 상세 조회",
            description = "빵 기록 ID 기준으로 개별 기록 상세와 카탈로그 빵 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<BreadRecordDetailResponse>> getBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId
    ) {
        BreadRecordDetailResponse response = breadRecordComplexService.getBreadRecord(
                resolveUserId(userId),
                recordId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 카탈로그에 존재하는 빵에 대해 새 기록을 생성합니다.
     *
     * @param userId 임시 사용자 ID
     * @param request 기존 빵 기록 생성 요청
     * @return 생성된 빵 기록 정보
     */
    @Operation(
            summary = "기존 빵 기록 생성",
            description = "카탈로그에 이미 존재하는 빵에 대해 새 기록을 생성합니다. multipart/form-data 필드명은 명세서와 동일하게 snake_case를 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "빵 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 카탈로그를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordCreateResponse>> createBreadRecord(
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId,
            @Valid @ModelAttribute CreateBreadRecordRequest request
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createBreadRecord(
                resolveUserId(userId),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 카탈로그에 없는 신규 빵을 추가하고 첫 기록을 생성합니다.
     *
     * @param userId 임시 사용자 ID
     * @param request 신규 빵 기록 생성 요청
     * @return 생성된 빵 기록 정보
     */
    @Operation(
            summary = "신규 빵 추가 및 기록 생성",
            description = "카탈로그에 없는 신규 빵을 추가하고 첫 기록을 생성합니다. multipart/form-data 필드명은 명세서와 동일하게 snake_case를 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "신규 빵 및 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 빵 이름"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(path = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordCreateResponse>> createNewBreadRecord(
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId,
            @Valid @ModelAttribute CreateNewBreadRecordRequest request
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createNewBreadRecord(
                resolveUserId(userId),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "개별 빵 기록 수정",
            description = "빵 기록 ID 기준으로 변경된 필드만 수정합니다. bread_id, name, bread_type은 수정할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 수정 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping(path = "/{recordId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BreadRecordDetailResponse>> updateBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId,
            @Valid @ModelAttribute UpdateBreadRecordRequest request
    ) {
        BreadRecordDetailResponse response = breadRecordComplexService.updateBreadRecord(
                resolveUserId(userId),
                recordId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "개별 빵 기록 삭제",
            description = "빵 기록 ID 기준으로 기록을 소프트 삭제하고 스티커 제거 여부를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "빵 기록 삭제 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordDeleteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "빵 기록을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<BreadRecordDeleteResponse>> deleteBreadRecord(
            @Parameter(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID recordId,
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId
    ) {
        BreadRecordDeleteResponse response = breadRecordComplexService.deleteBreadRecord(
                resolveUserId(userId),
                recordId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID resolveUserId(UUID userId) {
        return userId == null ? DUMMY_USER_ID : userId;
    }
}
