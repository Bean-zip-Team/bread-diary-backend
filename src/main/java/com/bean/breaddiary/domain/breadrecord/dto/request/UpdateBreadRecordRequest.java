package com.bean.breaddiary.domain.breadrecord.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 기록 수정 요청")
public class UpdateBreadRecordRequest {

    @Schema(description = "새 빵 사진 파일. 변경 시에만 전송", type = "string", format = "binary")
    private MultipartFile photo;

    @Schema(description = "구매처. 빈 문자열 전송 시 null로 초기화", example = "르뺑블루 성수점", maxLength = 50)
    @Size(max = 50, message = "구매처는 50자 이내로 입력해주세요.")
    private String shopName;

    @Schema(description = "먹은 날짜", example = "2026-04-16")
    private LocalDate eatenDate;

    @Schema(description = "별점", example = "5", minimum = "1", maximum = "5")
    @Min(value = 1, message = "별점은 1~5 사이 값이어야 합니다.")
    @Max(value = 5, message = "별점은 1~5 사이 값이어야 합니다.")
    private Integer rating;

    @Schema(description = "후기. 빈 문자열 전송 시 null로 초기화", example = "겉은 바삭하고 안은 촉촉해서 완벽했어요.", maxLength = 500)
    @Size(max = 500, message = "후기는 500자 이내로 입력해주세요.")
    private String review;
}
