package com.bean.breaddiary.domain.bread.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 프로필 기록 목록 아이템")
public class BreadProfileRecordResponse {

    @Schema(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "기록 사진 썸네일 URL", example = "https://cdn.bread-diary.app/bread-photos/record_thumb.webp")
    private String photoThumbnailUrl;

    @Schema(description = "별점", example = "5")
    private Integer rating;

    @Schema(description = "구매처", example = "르뺑블루 성수점", nullable = true)
    private String shopName;

    @Schema(description = "먹은 날짜", example = "2026-03-12")
    private LocalDate eatenDate;

    @Schema(description = "기록 생성 일시", example = "2026-03-12T09:30:00")
    private LocalDateTime createdAt;
}
