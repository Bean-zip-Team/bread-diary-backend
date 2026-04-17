package com.bean.breaddiary.domain.bread.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 도감 목록 조회 응답")
public class BreadCatalogListResponse {

    @ArraySchema(
            schema = @Schema(implementation = BreadCatalogItemResponse.class),
            arraySchema = @Schema(description = "빵 도감 목록")
    )
    private List<BreadCatalogItemResponse> items;

    @Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", example = "8", nullable = true)
    @JsonProperty("next_cursor")
    private String nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    @JsonProperty("has_more")
    private Boolean hasMore;

    @Schema(description = "필터 적용 후 전체 빵 수", example = "42")
    @JsonProperty("total_count")
    private Long totalCount;
}
