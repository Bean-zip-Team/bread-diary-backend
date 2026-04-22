package com.bean.breaddiary.domain.event.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventCollectResponse {

    @JsonProperty("accepted")
    private boolean accepted;
}
