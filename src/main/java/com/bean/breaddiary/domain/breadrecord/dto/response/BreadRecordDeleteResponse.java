package com.bean.breaddiary.domain.breadrecord.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "빵 기록 삭제 응답")
public class BreadRecordDeleteResponse {

    @Schema(description = "마지막 기록 삭제로 스티커가 제거되었는지 여부", example = "false")
    private Boolean stickerRemoved;
}
