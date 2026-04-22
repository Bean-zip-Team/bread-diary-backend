package com.bean.breaddiary.domain.event.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCollectRequest {

    @NotBlank(message = "eventName은 필수입니다.")
    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("occurredAt")
    private String occurredAt;

    @JsonProperty("anonymousId")
    private String anonymousId;
}
