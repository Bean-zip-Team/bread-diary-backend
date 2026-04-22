package com.bean.breaddiary.global.ratelimit;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public enum RateLimitPolicy {

    AUTH_TOSS("POST", "/auth/toss", 10, Duration.ofSeconds(60)),
    AUTH_REFRESH("POST", "/auth/refresh", 60, Duration.ofSeconds(60)),
    AUTH_TOSS_WEBHOOK("POST", "/auth/webhook/toss-unlink", 60, Duration.ofSeconds(60));

    private final String method;
    private final String path;
    private final int limit;
    private final Duration window;

    RateLimitPolicy(String method, String path, int limit, Duration window) {
        this.method = method;
        this.path = path;
        this.limit = limit;
        this.window = window;
    }

    public static Optional<RateLimitPolicy> find(String method, String path) {
        return Arrays.stream(values())
                .filter(policy -> policy.method.equalsIgnoreCase(method))
                .filter(policy -> policy.path.equals(path))
                .findFirst();
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public int limit() {
        return limit;
    }

    public Duration window() {
        return window;
    }

    public long windowSeconds() {
        return window.toSeconds();
    }
}
