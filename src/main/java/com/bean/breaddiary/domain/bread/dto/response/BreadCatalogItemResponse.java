package com.bean.breaddiary.domain.bread.dto.response;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 도감 목록 아이템")
public class BreadCatalogItemResponse {

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID breadId;

    @Schema(description = "도감 번호", example = "6")
    private Integer stickerNumber;

    @Schema(description = "빵 이름", example = "크루아상")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    private BreadType breadType;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/croissant.webp")
    private String imageUrl;

    @Schema(description = "현재 유저의 수집 여부", example = "true")
    private Boolean isCollected;

    @Schema(description = "현재 유저의 해당 빵 기록 수", example = "5")
    private Long eatCount;

    @Schema(description = "현재 유저의 해당 빵 평균 별점", example = "4.8", nullable = true)
    private Double avgRating;

    @Schema(description = "현재 유저의 해당 빵 최신 기록 썸네일 URL", example = "https://cdn.bread-diary.app/bread-photos/550e8400/a1b2c3d4_thumb.webp", nullable = true)
    private String latestPhotoUrl;

    @Schema(description = "현재 유저의 해당 빵 최신 먹은 날짜", example = "2026-03-14", nullable = true)
    private LocalDate latestEatenDate;
}
