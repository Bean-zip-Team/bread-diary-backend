package com.bean.breaddiary.domain.event.service;

import com.bean.breaddiary.global.common.ValidationFailureException;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum EventName {

    APP_OPENED("app_opened"),
    SCREEN_VIEWED("screen_viewed"),
    AUTH_TOSS_STARTED("auth_toss_started"),
    AUTH_TOSS_SUCCEEDED("auth_toss_succeeded"),
    BREAD_CATALOG_VIEWED("bread_catalog_viewed"),
    BREAD_DETAIL_VIEWED("bread_detail_viewed"),
    BREAD_RECORD_CREATE_STARTED("bread_record_create_started"),
    BREAD_RECORD_CREATE_SUCCEEDED("bread_record_create_succeeded"),
    LOGOUT_CLICKED("logout_clicked");

    private static final Pattern LOWER_SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$");
    private static final String INVALID_EVENT_NAME_MESSAGE = "허용되지 않는 이벤트 이름입니다.";

    private final String value;

    EventName(String value) {
        this.value = value;
    }

    public static EventName fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationFailureException("eventName은 필수입니다.");
        }

        String normalized = value.trim();
        if (!LOWER_SNAKE_CASE.matcher(normalized).matches()) {
            throw new ValidationFailureException(INVALID_EVENT_NAME_MESSAGE);
        }

        return Arrays.stream(values())
                .filter(eventName -> eventName.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ValidationFailureException(INVALID_EVENT_NAME_MESSAGE));
    }

    public String value() {
        return value;
    }
}
