package com.bean.breaddiary.domain.bread.dto.response;

import com.bean.breaddiary.domain.bread.entity.BreadType;
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
@Schema(description = "빵 카탈로그 자동완성 아이템")
public class BreadAutocompleteItemResponse {

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID breadId;

    @Schema(description = "빵 이름", example = "크루아상")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    private BreadType breadType;

    @Schema(description = "도감 번호", example = "6")
    private Integer stickerNumber;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/croissant.webp")
    private String imageUrl;

    @Schema(description = "현재 유저의 해당 빵 기록 수", example = "5")
    private Long eatCount;
}
