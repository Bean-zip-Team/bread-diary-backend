package com.bean.breaddiary.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossWebhookResponse {

    @JsonProperty("processed")
    private boolean processed;

    @JsonProperty("referrer")
    private String referrer;
}
