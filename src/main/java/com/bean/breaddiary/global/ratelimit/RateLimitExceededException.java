package com.bean.breaddiary.global.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final RateLimitPolicy policy;
    private final long retryAfterSeconds;

    public RateLimitExceededException(RateLimitPolicy policy, long retryAfterSeconds) {
        super("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        this.policy = policy;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitPolicy getPolicy() {
        return policy;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
