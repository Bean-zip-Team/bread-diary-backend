package com.bean.breaddiary.domain.breadrecord.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기존 카탈로그 빵 기록 생성 요청")
public class CreateBreadRecordRequest {

    @Schema(description = "카탈로그 빵 ID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "유효한 빵을 선택해주세요.")
    private UUID breadId;

    @Schema(description = "빵 사진 파일 (JPEG/PNG/WebP)", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사진은 필수입니다.")
    private MultipartFile photo;

    @Schema(description = "구매처", example = "르뺑블루 성수점", maxLength = 50)
    @Size(max = 50, message = "구매처는 50자 이내로 입력해주세요.")
    private String shopName;

    @Schema(description = "먹은 날짜. 미입력 시 오늘 날짜로 저장", example = "2026-04-16")
    private LocalDate eatenDate;

    @Schema(description = "별점", example = "5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1~5 사이 값이어야 합니다.")
    @Max(value = 5, message = "별점은 1~5 사이 값이어야 합니다.")
    private Integer rating;

    @Schema(description = "후기", example = "겉은 바삭하고 안은 촉촉해서 완벽했어요.", maxLength = 500)
    @Size(max = 500, message = "후기는 500자 이내로 입력해주세요.")
    private String review;
}
