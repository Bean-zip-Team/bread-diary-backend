package com.bean.breaddiary.domain.bread.dto.response;

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
@Schema(description = "빵 카탈로그 자동완성 응답")
public class BreadAutocompleteResponse {

    @ArraySchema(
            schema = @Schema(implementation = BreadAutocompleteItemResponse.class),
            arraySchema = @Schema(description = "자동완성 빵 목록")
    )
    private List<BreadAutocompleteItemResponse> items;

    @Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", example = "6", nullable = true)
    private String nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasMore;
}
