package com.bean.breaddiary.domain.auth.dto.response;

import com.bean.breaddiary.domain.auth.entity.TossWebhookEventType;
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

    @JsonProperty("eventType")
    private TossWebhookEventType eventType;
}
