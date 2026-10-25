package com.unique.ims.backend.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Redis-backed fixed-window rate limiter.
 *
 * <p>Two behaviours matter most and are both pinned here: the limit decision
 * itself, and the deliberate <em>fail-open</em> policy — if Redis is down the
 * API must stay available rather than locking every client out.</p>
 */
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitProperties properties;
    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setRequestsPerWindow(5);
        properties.setWindowSeconds(60);
        rateLimiter = new RateLimiterService(redisTemplate, properties);
    }

    /** Stubs the Lua script reply, which is the string {@code "<count>:<ttl>"}. */
    private void givenScriptReturns(long count, long ttl) {
        lenient().when(redisTemplate.execute(anyRedisScript(), anyList(), anyString()))
                .thenReturn(count + ":" + ttl);
    }

    /**
     * Typed matcher for the script argument. A bare {@code any(RedisScript.class)}
     * would return the raw type and produce an unchecked warning of its own.
     */
    private static RedisScript<String> anyRedisScript() {
        return ArgumentMatchers.any();
    }

    @Test
    @DisplayName("allows a request below the limit and reports the remaining allowance")
    void allowsRequestBelowLimit() {
        givenScriptReturns(3, 45);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(5);
        assertThat(decision.remaining()).isEqualTo(2);
        assertThat(decision.retryAfterSeconds()).isEqualTo(45);
        assertThat(decision.resetEpochSeconds()).isPositive();
    }

    @Test
    @DisplayName("allows the request that exactly consumes the last token")
    void allowsRequestAtExactLimit() {
        givenScriptReturns(5, 30);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
    }

    @Test
    @DisplayName("rejects the request that exceeds the limit")
    void rejectsRequestOverLimit() {
        givenScriptReturns(6, 20);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isEqualTo(20);
    }

    @Test
    @DisplayName("never reports a negative remaining count when well over the limit")
    void remainingNeverGoesNegative() {
        givenScriptReturns(99, 10);

        assertThat(rateLimiter.check("10.0.0.1").remaining()).isZero();
    }

    @Test
    @DisplayName("falls back to the configured window when Redis reports no expiry")
    void fallsBackWhenTtlMissing() {
        givenScriptReturns(1, -1);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.retryAfterSeconds()).isEqualTo(60);
        assertThat(decision.allowed()).isTrue();
    }

    @Test
    @DisplayName("short-circuits without touching Redis when limiting is disabled")
    void disabledLimiterSkipsRedis() {
        properties.setEnabled(false);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(5);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("fails open when Redis is unreachable so the API stays available")
    void failsOpenWhenRedisIsDown() {
        when(redisTemplate.execute(anyRedisScript(), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(5);
    }

    @Test
    @DisplayName("fails open on an unexpected script reply shape")
    void failsOpenOnUnexpectedReply() {
        lenient().when(redisTemplate.execute(anyRedisScript(), anyList(), anyString()))
                .thenReturn(null);

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    @DisplayName("fails open when the script reply cannot be parsed")
    void failsOpenOnMalformedReply() {
        lenient().when(redisTemplate.execute(anyRedisScript(), anyList(), anyString()))
                .thenReturn("not-a-valid-reply");

        RateLimitDecision decision = rateLimiter.check("10.0.0.1");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(5);
    }
}
