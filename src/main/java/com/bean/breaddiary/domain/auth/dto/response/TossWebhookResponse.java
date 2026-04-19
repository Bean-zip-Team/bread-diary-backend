package com.bean.breaddiary.domain.auth.dto.response;

import com.bean.breaddiary.domain.auth.entity.TossWebhookEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossWebhookResponse {

    private boolean processed;
    private TossWebhookEventType eventType;
}
