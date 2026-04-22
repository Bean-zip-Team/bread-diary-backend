package com.bean.breaddiary.global.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryRateLimiter {

    private static final long CLEANUP_INTERVAL_MILLIS = 60_000L;

    private final Map<RateLimitKey, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;
    private volatile long lastCleanupMillis;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public RateLimitResult consume(RateLimitPolicy policy, String identifier) {
        long nowMillis = clock.millis();
        cleanupExpiredBuckets(nowMillis);

        RateLimitKey key = new RateLimitKey(policy, identifier);
        AtomicReference<RateLimitResult> result = new AtomicReference<>();

        buckets.compute(key, (ignored, bucket) -> {
            if (bucket == null || isExpired(bucket, policy, nowMillis)) {
                result.set(RateLimitResult.allowedResult());
                return new Bucket(nowMillis, 1);
            }

            if (bucket.count() < policy.limit()) {
                result.set(RateLimitResult.allowedResult());
                return new Bucket(bucket.windowStartMillis(), bucket.count() + 1);
            }

            result.set(RateLimitResult.blockedResult(retryAfterSeconds(bucket, policy, nowMillis)));
            return bucket;
        });

        return result.get();
    }

    private void cleanupExpiredBuckets(long nowMillis) {
        if (nowMillis - lastCleanupMillis < CLEANUP_INTERVAL_MILLIS) {
            return;
        }

        lastCleanupMillis = nowMillis;
        long maxWindowMillis = maxWindowMillis();
        buckets.entrySet()
                .removeIf(entry -> nowMillis - entry.getValue().windowStartMillis() > maxWindowMillis * 2);
    }

    private long maxWindowMillis() {
        long maxWindowMillis = 0;
        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            maxWindowMillis = Math.max(maxWindowMillis, policy.window().toMillis());
        }
        return maxWindowMillis;
    }

    private boolean isExpired(Bucket bucket, RateLimitPolicy policy, long nowMillis) {
        return nowMillis - bucket.windowStartMillis() >= policy.window().toMillis();
    }

    private long retryAfterSeconds(Bucket bucket, RateLimitPolicy policy, long nowMillis) {
        long elapsedMillis = nowMillis - bucket.windowStartMillis();
        long remainingMillis = Math.max(0, policy.window().toMillis() - elapsedMillis);

        return Math.max(1, (long) Math.ceil(remainingMillis / 1000.0));
    }

    public record RateLimitResult(
            boolean allowed,
            long retryAfterSeconds
    ) {

        private static RateLimitResult allowedResult() {
            return new RateLimitResult(true, 0);
        }

        private static RateLimitResult blockedResult(long retryAfterSeconds) {
            return new RateLimitResult(false, retryAfterSeconds);
        }
    }

    private record RateLimitKey(
            RateLimitPolicy policy,
            String identifier
    ) {
    }

    private record Bucket(
            long windowStartMillis,
            int count
    ) {
    }
}
