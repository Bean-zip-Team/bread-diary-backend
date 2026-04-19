package com.bean.breaddiary.domain.user.controller;

import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserWithdrawalResponse;
import com.bean.breaddiary.domain.user.service.UserService;
import com.bean.breaddiary.domain.user.service.UserWithdrawalService;
import com.bean.breaddiary.global.common.ApiResponse;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "사용자 API", description = "현재 로그인한 사용자 정보를 조회하고 탈퇴를 처리하는 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;

    @Operation(
            summary = "내 프로필 조회",
            description = "인증된 Access Token 기준으로 현재 사용자 프로필과 기록 통계를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserMeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getCurrentUserProfile(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        UUID userId = AuthRequestAttributes.getRequiredUserId(request);
        UserMeResponse response = userService.getCurrentUserProfile(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "토스 연결 끊기를 요청한 뒤 콜백을 기다리지 않고 로컬 사용자 데이터를 즉시 완전 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = UserWithdrawalResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<UserWithdrawalResponse>> withdrawCurrentUser(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        UUID userId = AuthRequestAttributes.getRequiredUserId(request);
        UserWithdrawalResponse response = userWithdrawalService.withdrawCurrentUser(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
