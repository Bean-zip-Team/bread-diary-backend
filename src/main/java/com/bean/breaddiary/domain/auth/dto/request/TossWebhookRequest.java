package com.bean.breaddiary.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossWebhookRequest {

    @NotBlank(message = "user_key는 필수입니다.")
    @JsonAlias("userKey")
    private String userKey;

    @NotBlank(message = "referrer는 필수입니다.")
    @JsonAlias("eventType")
    private String referrer;
}
