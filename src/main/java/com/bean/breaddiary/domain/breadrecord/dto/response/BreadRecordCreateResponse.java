package com.bean.breaddiary.domain.breadrecord.dto.response;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "빵 기록 생성 응답")
public class BreadRecordCreateResponse {

    @Schema(description = "빵 기록 ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    @JsonProperty("bread_id")
    private UUID breadId;

    @Schema(description = "도감 번호", example = "7")
    @JsonProperty("sticker_number")
    private Integer stickerNumber;

    @Schema(description = "원본 사진 URL", example = "https://breaddiary-bucket.s3.ap-northeast-2.amazonaws.com/bread/550e8400/a1b2c3d4.webp")
    @JsonProperty("photo_url")
    private String photoUrl;

    @Schema(description = "썸네일 사진 URL", example = "https://breaddiary-bucket.s3.ap-northeast-2.amazonaws.com/bread/550e8400/a1b2c3d4_thumb.webp")
    @JsonProperty("photo_thumbnail_url")
    private String photoThumbnailUrl;

    @Schema(description = "빵 이름", example = "크루아상")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    @JsonProperty("bread_type")
    private BreadType breadType;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/croissant.webp")
    @JsonProperty("image_url")
    private String imageUrl;

    @Schema(description = "구매처", example = "르뺑블루 성수점")
    @JsonProperty("shop_name")
    private String shopName;

    @Schema(description = "먹은 날짜", example = "2026-04-16")
    @JsonProperty("eaten_date")
    private LocalDate eatenDate;

    @Schema(description = "별점", example = "5")
    private Integer rating;

    @Schema(description = "후기", example = "겉은 바삭하고 안은 촉촉해서 완벽했어요.")
    private String review;

    @Schema(description = "해당 빵의 첫 기록 여부", example = "false")
    @JsonProperty("is_first_record")
    private Boolean isFirstRecord;

    @Schema(description = "기록 생성 일시", example = "2026-04-16T09:30:00")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
