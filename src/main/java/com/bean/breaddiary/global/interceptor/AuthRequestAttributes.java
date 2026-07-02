package com.bean.breaddiary.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class AuthRequestAttributes {

    public static final String USER_ID = "authenticatedUserId";
    public static final String SESSION_ID = "authenticatedSessionId";

    private AuthRequestAttributes() {
    }

    public static UUID getRequiredUserId(HttpServletRequest request) {
        return getRequiredUuid(request, USER_ID);
    }

    public static UUID getRequiredSessionId(HttpServletRequest request) {
        return getRequiredUuid(request, SESSION_ID);
    }

    public static UUID getOptionalUserId(HttpServletRequest request) {
        return getOptionalUuid(request, USER_ID);
    }

    public static UUID getOptionalSessionId(HttpServletRequest request) {
        return getOptionalUuid(request, SESSION_ID);
    }

    private static UUID getRequiredUuid(HttpServletRequest request, String attributeName) {
        UUID value = getOptionalUuid(request, attributeName);

        if (value != null) {
            return value;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    private static UUID getOptionalUuid(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);

        if (value instanceof UUID uuid) {
            return uuid;
        }

        return null;
    }
}
