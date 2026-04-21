package com.bean.breaddiary.domain.breadtype.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 종류 아이템")
public class BreadTypeItemResponse {

    @Schema(description = "빵 종류 코드", example = "PASTRY")
    private String code;

    @Schema(description = "빵 종류 한글 표시명", example = "페이스트리")
    private String label;
}
