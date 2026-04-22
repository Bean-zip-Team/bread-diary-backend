package com.bean.breaddiary.global.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    private MutableClock clock;
    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-04-22T00:00:00Z"));
        rateLimiter = new InMemoryRateLimiter(clock);
    }

    @Test
    void consumeAllowsRequestsWithinLimit() {
        for (int i = 0; i < RateLimitPolicy.AUTH_TOSS.limit(); i++) {
            InMemoryRateLimiter.RateLimitResult result = rateLimiter.consume(
                    RateLimitPolicy.AUTH_TOSS,
                    "127.0.0.1"
            );

            assertTrue(result.allowed());
        }
    }

    @Test
    void consumeBlocksRequestOverLimit() {
        for (int i = 0; i < RateLimitPolicy.AUTH_TOSS.limit(); i++) {
            rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1");
        }

        InMemoryRateLimiter.RateLimitResult result = rateLimiter.consume(
                RateLimitPolicy.AUTH_TOSS,
                "127.0.0.1"
        );

        assertFalse(result.allowed());
        assertEquals(60, result.retryAfterSeconds());
    }

    @Test
    void consumeAllowsAgainAfterWindowExpires() {
        for (int i = 0; i < RateLimitPolicy.AUTH_TOSS.limit(); i++) {
            rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1");
        }
        assertFalse(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1").allowed());

        clock.advance(Duration.ofSeconds(60));

        assertTrue(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1").allowed());
    }

    @Test
    void consumeSeparatesBucketsByEndpoint() {
        for (int i = 0; i < RateLimitPolicy.AUTH_TOSS.limit(); i++) {
            rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1");
        }
        assertFalse(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1").allowed());

        assertTrue(rateLimiter.consume(RateLimitPolicy.AUTH_REFRESH, "127.0.0.1").allowed());
    }

    @Test
    void consumeSeparatesBucketsByIdentifier() {
        for (int i = 0; i < RateLimitPolicy.AUTH_TOSS.limit(); i++) {
            rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1");
        }
        assertFalse(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.1").allowed());

        assertTrue(rateLimiter.consume(RateLimitPolicy.AUTH_TOSS, "127.0.0.2").allowed());
    }

    @Test
    void authRefreshPolicyUsesSixtyRequestsPerSixtySeconds() {
        assertEquals(60, RateLimitPolicy.AUTH_REFRESH.limit());
        assertEquals(60, RateLimitPolicy.AUTH_REFRESH.windowSeconds());
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
