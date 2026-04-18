package com.bean.breaddiary.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 기록 통계 응답")
public class UserStatsResponse {

    @Schema(description = "삭제되지 않은 전체 기록 수", example = "42")
    private Long totalRecords;

    @Schema(description = "삭제되지 않은 기록 기준 고유 구매처 수", example = "18")
    private Long uniqueShops;

    @Schema(description = "삭제되지 않은 기록 기준 평균 평점", example = "4.2", nullable = true)
    private Double avgRating;
}
