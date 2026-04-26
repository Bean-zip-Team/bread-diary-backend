package com.bean.breaddiary.domain.auth.controller;

import com.bean.breaddiary.domain.auth.dto.request.LogoutRequest;
import com.bean.breaddiary.domain.auth.dto.request.RefreshTokenRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossLoginRequest;
import com.bean.breaddiary.domain.auth.dto.request.TossWebhookRequest;
import com.bean.breaddiary.domain.auth.dto.response.AuthTokenResponse;
import com.bean.breaddiary.domain.auth.dto.response.LogoutResponse;
import com.bean.breaddiary.domain.auth.dto.response.TossWebhookResponse;
import com.bean.breaddiary.domain.auth.service.AuthService;
import com.bean.breaddiary.domain.user.service.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 API", description = "로그인, 토큰 재발급, 로그아웃, 토스 웹훅 처리 API입니다.")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String TOSS_WEBHOOK_SECRET_HEADER = "x-toss-webhook-secret";

    private final AuthService authService;
    private final UserService userService;

    @Operation(
            summary = "토스 로그인",
            description = "토스 authorization code를 검증하고 Access / Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토스 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/toss")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> loginWithToss(
            @Valid @RequestBody TossLoginRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.loginWithToss(request))
        );
    }

    @Operation(
            summary = "토큰 재발급",
            description = "유효한 리프레시 토큰으로 Access / Refresh Token을 모두 재발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 회전된 리프레시 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.refresh(request))
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "Access Token의 현재 세션을 종료합니다. refreshToken이 전달되면 해당 토큰도 함께 검증합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = LogoutResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 회전된 리프레시 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            return ResponseEntity.ok(
                    ApiResponse.success(authService.logout(request))
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(authService.logoutCurrentSession(
                        AuthRequestAttributes.getRequiredSessionId(httpServletRequest)
                ))
        );
    }

    @Operation(
            summary = "토스 연결 해제 웹훅",
            description = "토스 웹훅 secret을 검증하고 eventType에 따라 세션 정리 또는 사용자 완전 삭제를 수행합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "웹훅 처리 성공",
                    content = @Content(schema = @Schema(implementation = TossWebhookResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "웹훅 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 웹훅 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/webhook/toss-unlink")
    public ResponseEntity<ApiResponse<TossWebhookResponse>> handleTossWebhook(
            @RequestHeader(TOSS_WEBHOOK_SECRET_HEADER) String webhookSecret,
            @Valid @RequestBody TossWebhookRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.handleTossWebhook(webhookSecret, request))
        );
    }
}
