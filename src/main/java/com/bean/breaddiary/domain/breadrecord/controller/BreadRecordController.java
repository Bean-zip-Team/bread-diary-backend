package com.bean.breaddiary.domain.breadrecord.controller;

import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordComplexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * 카탈로그에 존재하는 빵에 대해 새 기록을 생성합니다.
     *
     * @param userId 임시 사용자 ID
     * @param request 기존 빵 기록 생성 요청
     * @return 생성된 빵 기록 정보
     */
    @Operation(
            summary = "기존 빵 기록 생성",
            description = "카탈로그에 이미 존재하는 빵에 대해 새 기록을 생성합니다. multipart/form-data 필드명은 camelCase를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "빵 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "빵 카탈로그를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BreadRecordCreateResponse> createBreadRecord(
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId,
            @Valid @ModelAttribute CreateBreadRecordRequest request
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createBreadRecord(
                resolveUserId(userId),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            description = "카탈로그에 없는 신규 빵을 추가하고 첫 기록을 생성합니다. multipart/form-data 필드명은 camelCase를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "신규 빵 및 기록 생성 성공",
                    content = @Content(schema = @Schema(implementation = BreadRecordCreateResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "중복된 빵 이름"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(path = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BreadRecordCreateResponse> createNewBreadRecord(
            @Parameter(description = "임시 사용자 ID. user/auth 연동 전까지 사용합니다.", example = "00000000-0000-0000-0000-000000000001")
            @RequestHeader(value = "X-USER-ID", required = false) UUID userId,
            @Valid @ModelAttribute CreateNewBreadRecordRequest request
    ) {
        BreadRecordCreateResponse response = breadRecordComplexService.createNewBreadRecord(
                resolveUserId(userId),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID resolveUserId(UUID userId) {
        return userId == null ? DUMMY_USER_ID : userId;
    }
}
