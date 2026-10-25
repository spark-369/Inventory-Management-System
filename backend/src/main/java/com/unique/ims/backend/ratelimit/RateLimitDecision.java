package com.unique.ims.backend.ratelimit;

/**
 * Outcome of a single rate-limit check.
 *
 * @param allowed            whether the request may proceed
 * @param limit              the configured ceiling for the window
 * @param remaining          requests left in the current window
 * @param retryAfterSeconds  seconds the client should wait before retrying
 * @param resetEpochSeconds  unix time (seconds) at which the window resets
 */
public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        long resetEpochSeconds
) {

    /** Fail-open decision used when Redis is unreachable. */
    public static RateLimitDecision unlimited(long limit) {
        return new RateLimitDecision(true, limit, limit, 0L, 0L);
    }
}
