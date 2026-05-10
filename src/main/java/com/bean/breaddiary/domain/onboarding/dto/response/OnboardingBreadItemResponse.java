package com.bean.breaddiary.domain.onboarding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "온보딩 빵 목록 아이템")
public class OnboardingBreadItemResponse {

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID breadId;

    @Schema(description = "온보딩 표시용 빵 이름", example = "식빵")
    private String name;

    @Schema(description = "도감 번호", example = "41")
    private Integer stickerNumber;

    @Schema(description = "빵 종류", example = "BREAD")
    private String breadType;

    @Schema(description = "원본 카탈로그 이미지 URL", example = "https://du4zizlgiw14n.cloudfront.net/images/041_%EC%83%9D%EC%8B%9D%EB%B9%B5.png")
    private String imageUrl;
}
