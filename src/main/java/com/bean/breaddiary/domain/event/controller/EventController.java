package com.bean.breaddiary.domain.event.controller;

import com.bean.breaddiary.domain.event.dto.request.EventCollectRequest;
import com.bean.breaddiary.domain.event.dto.response.EventCollectResponse;
import com.bean.breaddiary.domain.event.service.EventCollectionService;
import com.bean.breaddiary.global.common.ApiResponse;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventCollectionService eventCollectionService;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<EventCollectResponse>> collectEvent(
            @Valid @RequestBody EventCollectRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UUID userId = AuthRequestAttributes.getOptionalUserId(httpServletRequest);
        UUID sessionId = AuthRequestAttributes.getOptionalSessionId(httpServletRequest);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(eventCollectionService.collect(request, userId, sessionId)));
    }
}
