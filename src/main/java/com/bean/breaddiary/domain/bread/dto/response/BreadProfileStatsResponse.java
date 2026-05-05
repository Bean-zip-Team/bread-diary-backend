package com.bean.breaddiary.domain.bread.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 프로필 통계")
public class BreadProfileStatsResponse {

    @Schema(description = "현재 유저의 해당 빵 실제 기록 수. 온보딩만 선택한 경우 0일 수 있습니다.", example = "5")
    private Long eatCount;

    @Schema(description = "현재 유저의 해당 빵 평균 별점", example = "4.8", nullable = true)
    private Double avgRating;

    @Schema(description = "현재 유저가 해당 빵을 처음 기록한 일시", example = "2026-03-14T09:30:00", nullable = true)
    private LocalDateTime firstRecordedAt;
}
