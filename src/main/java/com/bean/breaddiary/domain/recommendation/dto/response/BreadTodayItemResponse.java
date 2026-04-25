package com.bean.breaddiary.domain.recommendation.dto.response;

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
@Schema(description = "오늘의 추천 빵 아이템")
public class BreadTodayItemResponse {

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID id;

    @Schema(description = "빵 이름", example = "소금빵")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    private String type;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/salt-bread.webp")
    private String imageUrl;

    @Schema(description = "전체 활성 기록 수", example = "128")
    private Long totalRecordCount;

    @Schema(description = "현재 유저 수집 여부", example = "false")
    private Boolean isCollected;
}
