package com.unique.ims.backend.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis.
 *
 * <p>The counter increment and the TTL assignment run inside a single Lua
 * script, so the check is atomic: two concurrent requests can never both read
 * the same pre-increment value and slip past the limit (a plain
 * {@code INCR} + {@code EXPIRE} pair can, if the process dies between the two
 * calls, leave a key with no expiry that blocks a client forever).</p>
 *
 * <p>Fixed windows are used deliberately: they cost one Redis round trip and one
 * key per client, which is the right trade-off for protecting an internal
 * inventory API. A sliding window would need a sorted set per client.</p>
 *
 * <p>If Redis is unavailable the limiter <em>fails open</em> — availability of the
 * API takes precedence over strictness, and the failure is logged.</p>
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final String KEY_PREFIX = "ims::ratelimit::";

    /**
     * Increments the window counter and returns {@code "<count>:<ttlSeconds>"}.
     * The TTL is only set when the key is created, so the window does not slide
     * on every request.
     *
     * <p>The script returns a single concatenated string rather than a Lua table.
     * That is a deliberate type-safety choice: a table reply would force the
     * script to be typed as {@code RedisScript<List<Long>>}, and because Java has
     * no {@code Class<List<Long>>} literal, that in turn requires an unchecked
     * cast at declaration plus an unchecked invocation of
     * {@code RedisTemplate.execute}. Returning {@code String} lets the script be
     * fully parameterised with a real class literal ({@code String.class}), so no
     * suppression is needed anywhere.</p>
     */
    private static final RedisScript<String> SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return current .. ':' .. ttl
            """,
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Consumes one token for the given client.
     *
     * @param clientKey stable identifier for the caller (IP address, API key, …)
     */
    public RateLimitDecision check(String clientKey) {
        long limit = properties.getRequestsPerWindow();

        if (!properties.isEnabled()) {
            return RateLimitDecision.unlimited(limit);
        }

        String redisKey = KEY_PREFIX + clientKey;
        try {
            String reply = redisTemplate.execute(
                    SCRIPT,
                    List.of(redisKey),
                    String.valueOf(properties.getWindowSeconds()));

            if (reply == null || reply.isBlank()) {
                // Unexpected reply shape — do not lock the client out over it.
                return RateLimitDecision.unlimited(limit);
            }

            // "<count>:<ttl>" — the script guarantees the separator.
            int separator = reply.indexOf(':');
            if (separator < 0) {
                return RateLimitDecision.unlimited(limit);
            }

            long count = Long.parseLong(reply.substring(0, separator));
            long ttl = Long.parseLong(reply.substring(separator + 1));
            if (ttl < 0) {
                // No expiry was applied (should not happen); fall back to the window.
                ttl = properties.getWindowSeconds();
            }

            long remaining = Math.max(0L, limit - count);
            long resetEpochSeconds = Instant.now().getEpochSecond() + ttl;

            return new RateLimitDecision(count <= limit, limit, remaining, ttl, resetEpochSeconds);
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable, allowing request for client '{}': {}", clientKey, ex.getMessage());
            return RateLimitDecision.unlimited(limit);
        }
    }
}
