package com.bean.breaddiary.domain.event.service;

import com.bean.breaddiary.global.common.ValidationFailureException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class EventPropertySanitizer {

    public static final String SENSITIVE_PROPERTY_MESSAGE = "이벤트 속성에 허용되지 않는 항목이 포함되어 있습니다.";

    private static final int MAX_PROPERTY_COUNT = 30;
    private static final int MAX_KEY_LENGTH = 60;
    private static final int MAX_STRING_VALUE_LENGTH = 200;
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][A-Za-z0-9_]*$");
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password",
            "token",
            "refresh_token",
            "access_token",
            "authorization",
            "cookie",
            "email",
            "phone"
    );

    public Map<String, Object> sanitize(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Map.of();
        }

        if (properties.size() > MAX_PROPERTY_COUNT) {
            throw new ValidationFailureException("이벤트 속성 개수가 허용 범위를 초과했습니다.");
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            validateKey(key);
            sanitized.put(key, sanitizeValue(value));
        });

        return Collections.unmodifiableMap(sanitized);
    }

    private void validateKey(String key) {
        if (!StringUtils.hasText(key)
                || key.length() > MAX_KEY_LENGTH
                || !KEY_PATTERN.matcher(key).matches()) {
            throw new ValidationFailureException("이벤트 속성 이름이 올바르지 않습니다.");
        }

        String normalizedKey = key.toLowerCase(Locale.ROOT);
        boolean sensitive = SENSITIVE_KEY_PARTS.stream()
                .anyMatch(normalizedKey::contains);

        if (sensitive) {
            throw new ValidationFailureException(SENSITIVE_PROPERTY_MESSAGE);
        }
    }

    private Object sanitizeValue(Object value) {
        if (value == null || value instanceof Boolean || value instanceof Number) {
            return value;
        }

        if (value instanceof String stringValue) {
            if (stringValue.length() > MAX_STRING_VALUE_LENGTH) {
                throw new ValidationFailureException("이벤트 속성 값이 허용 길이를 초과했습니다.");
            }

            return stringValue;
        }

        throw new ValidationFailureException("이벤트 속성 값 형식이 올바르지 않습니다.");
    }
}
