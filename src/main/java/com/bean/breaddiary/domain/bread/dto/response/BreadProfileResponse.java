package com.bean.breaddiary.domain.bread.dto.response;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 프로필 조회 응답")
public class BreadProfileResponse {

    @Schema(description = "카탈로그 빵 ID", example = "b0e1f2a3-c4d5-6789-abcd-ef0123456789")
    private UUID breadId;

    @Schema(description = "도감 번호", example = "6")
    private Integer stickerNumber;

    @Schema(description = "빵 이름", example = "크루아상")
    private String name;

    @Schema(description = "빵 종류", example = "PASTRY")
    private BreadType breadType;

    @Schema(description = "빵 종류 한글 표시명", example = "페이스트리")
    private String breadTypeLabel;

    @Schema(description = "카탈로그 이미지 URL", example = "https://cdn.bread-diary.app/breads/croissant.webp")
    private String imageUrl;

    @Schema(description = "현재 유저의 해당 빵 통계")
    private BreadProfileStatsResponse stats;

    @ArraySchema(
            schema = @Schema(implementation = BreadProfileRecordResponse.class),
            arraySchema = @Schema(description = "현재 유저의 해당 빵 기록 목록")
    )
    private List<BreadProfileRecordResponse> records;
}
