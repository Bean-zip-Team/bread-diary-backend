package com.bean.breaddiary.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "오늘의 추천 빵 응답")
public class BreadTodayResponse {

    @Schema(description = "추천 날짜", example = "2026-04-26")
    private LocalDate date;

    @ArraySchema(
            schema = @Schema(implementation = BreadTodayItemResponse.class),
            arraySchema = @Schema(description = "오늘의 추천 빵 목록")
    )
    private List<BreadTodayItemResponse> breads;
}
