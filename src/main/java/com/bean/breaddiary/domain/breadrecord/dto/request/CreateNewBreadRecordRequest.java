package com.bean.breaddiary.domain.breadrecord.dto.request;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "신규 빵 추가 및 첫 기록 생성 요청")
public class CreateNewBreadRecordRequest {

    @Schema(description = "빵 이름", example = "말차 크로플", maxLength = 30, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "빵 이름은 필수입니다.")
    @Size(max = 30, message = "빵 이름은 30자 이내로 입력해주세요.")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
            "PASTRY", "BREAD", "DONUT", "CAKE", "BAGEL", "TART", "OTHER"
    })
    @NotNull(message = "올바른 빵 종류를 선택해주세요.")
    private BreadType breadType;

    @Schema(description = "빵 사진 파일 (JPEG/PNG/WebP)", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사진은 필수입니다.")
    private MultipartFile photo;

    @Schema(description = "구매처", example = "카페봄봄 합정점", maxLength = 50)
    @Size(max = 50, message = "구매처는 50자 이내로 입력해주세요.")
    private String shopName;

    @Schema(description = "먹은 날짜. 미입력 시 오늘 날짜로 저장", example = "2026-04-16")
    private LocalDate eatenDate;

    @Schema(description = "별점", example = "4", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1~5 사이 값이어야 합니다.")
    @Max(value = 5, message = "별점은 1~5 사이 값이어야 합니다.")
    private Integer rating;

    @Schema(description = "후기", example = "말차 맛이 진해서 좋았어요.", maxLength = 500)
    @Size(max = 500, message = "후기는 500자 이내로 입력해주세요.")
    private String review;

    public void setBread_type(BreadType breadType) {
        this.breadType = breadType;
    }

    public void setShop_name(String shopName) {
        this.shopName = shopName;
    }

    public void setEaten_date(LocalDate eatenDate) {
        this.eatenDate = eatenDate;
    }
}
