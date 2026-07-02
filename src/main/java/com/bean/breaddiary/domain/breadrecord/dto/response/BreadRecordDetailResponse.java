package com.bean.breaddiary.domain.breadrecord.dto.response;
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
@Schema(description = "빵 기록 상세 응답")
public class BreadRecordDetailResponse {

    @Schema(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID breadId;

    @Schema(description = "도감 번호", example = "7")
    private Integer stickerNumber;

    @Schema(description = "빵 이름", example = "크루아상")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    private String breadType;

    @Schema(description = "빵 종류 한글 표시명", example = "페이스트리")
    private String breadTypeLabel;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/croissant.webp")
    private String imageUrl;

    @Schema(description = "원본 사진 URL", example = "https://cdn.bread-diary.app/bread-photos/record.webp")
    private String photoUrl;

    @Schema(description = "썸네일 사진 URL", example = "https://cdn.bread-diary.app/bread-photos/record_thumb.webp")
    private String photoThumbnailUrl;

    @Schema(description = "구매처", example = "르뺑블루 성수점", nullable = true)
    private String shopName;

    @Schema(description = "먹은 날짜", example = "2026-03-14")
    private LocalDate eatenDate;

    @Schema(description = "별점", example = "5")
    private Integer rating;

    @Schema(description = "후기", example = "겉은 바삭하고 안은 촉촉해서 완벽했어요.", nullable = true)
    private String review;

    @Schema(description = "기록 생성 일시", example = "2026-03-14T09:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "기록 수정 일시", example = "2026-03-14T10:15:00")
    private LocalDateTime updatedAt;
}
