package com.bean.breaddiary.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현재 사용자 정보 응답")
public class UserMeResponse {

    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("id")
    private UUID id;

    @Schema(description = "닉네임", example = "빵순이")
    @JsonProperty("nickname")
    private String nickname;

    @Schema(description = "이메일", example = "bread@toss.im")
    @JsonProperty("email")
    private String email;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.breaddex.app/profiles/550e8400.webp", nullable = true)
    @JsonProperty("profileImageUrl")
    private String profileImageUrl;

    @Schema(description = "자기소개", example = "오늘도 빵을 먹습니다.", nullable = true)
    @JsonProperty("bio")
    private String bio;

    @Schema(description = "사용자 기록 통계")
    @JsonProperty("stats")
    private UserStatsResponse stats;

    @Schema(description = "계정 생성 일시", example = "2026-04-01T00:00:00", nullable = true)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
