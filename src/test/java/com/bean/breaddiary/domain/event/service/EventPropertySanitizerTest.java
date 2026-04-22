package com.bean.breaddiary.domain.event.service;

import com.bean.breaddiary.global.common.ValidationFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventPropertySanitizerTest {

    private EventPropertySanitizer eventPropertySanitizer;

    @BeforeEach
    void setUp() {
        eventPropertySanitizer = new EventPropertySanitizer();
    }

    @Test
    void sanitizeReturnsEmptyMapForNullProperties() {
        assertEquals(Map.of(), eventPropertySanitizer.sanitize(null));
    }

    @Test
    void sanitizeAllowsPrimitiveValuesAndNull() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("screen", "home");
        properties.put("count", 1);
        properties.put("enabled", true);
        properties.put("emptyValue", null);

        Map<String, Object> sanitized = eventPropertySanitizer.sanitize(properties);

        assertEquals("home", sanitized.get("screen"));
        assertEquals(1, sanitized.get("count"));
        assertEquals(true, sanitized.get("enabled"));
        assertEquals(null, sanitized.get("emptyValue"));
    }

    @Test
    void sanitizeRejectsTooManyProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        IntStream.rangeClosed(1, 31)
                .forEach(index -> properties.put("key" + index, index));

        assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(properties)
        );
    }

    @Test
    void sanitizeRejectsTooLongKey() {
        assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(Map.of("a".repeat(61), "value"))
        );
    }

    @Test
    void sanitizeRejectsTooLongStringValue() {
        assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(Map.of("screen", "a".repeat(201)))
        );
    }

    @Test
    void sanitizeRejectsObjectAndArrayValues() {
        assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(Map.of("nested", Map.of("key", "value")))
        );

        assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(Map.of("items", List.of("a", "b")))
        );
    }

    @Test
    void sanitizeRejectsSensitiveKeysWithoutExposingKeyInMessage() {
        ValidationFailureException exception = assertThrows(
                ValidationFailureException.class,
                () -> eventPropertySanitizer.sanitize(Map.of("accessToken", "secret"))
        );

        assertEquals(EventPropertySanitizer.SENSITIVE_PROPERTY_MESSAGE, exception.getMessage());
        assertFalse(exception.getMessage().contains("accessToken"));
    }
}
