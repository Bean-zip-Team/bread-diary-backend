package com.bean.breaddiary.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그아웃 응답")
public class LogoutResponse {

    @Schema(description = "로그아웃 성공 여부", example = "true")
    private boolean loggedOut;
}
