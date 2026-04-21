package com.bean.breaddiary.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawalResponse {

    @JsonProperty("withdrawn")
    private boolean withdrawn;
}
