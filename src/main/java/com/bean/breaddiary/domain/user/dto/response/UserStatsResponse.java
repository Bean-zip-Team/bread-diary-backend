package com.bean.breaddiary.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("totalRecords")
    private Long totalRecords;

    @Schema(description = "삭제되지 않은 기록 기준 고유 구매처 수", example = "18")
    @JsonProperty("uniqueShops")
    private Long uniqueShops;

    @Schema(description = "삭제되지 않은 기록 기준 평균 평점", example = "4.2", nullable = true)
    @JsonProperty("avgRating")
    private Double avgRating;
}
