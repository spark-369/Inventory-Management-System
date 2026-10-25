package com.unique.ims.backend.ratelimit;

/**
 * Thrown when a client exceeds its request allowance.
 *
 * <p>Extends {@link RuntimeException} and is translated to an HTTP 429 response
 * by {@link com.unique.ims.backend.exception.GlobalExceptionHandler}, so rate-limit
 * rejections use the same structured {@code ApiErrorResponse} body as every other
 * error in this application.</p>
 */
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitDecision decision;

    public RateLimitExceededException(RateLimitDecision decision) {
        super("Too many requests. Limit is " + decision.limit()
                + " requests per window; retry in " + decision.retryAfterSeconds() + " second(s).");
        this.decision = decision;
    }

    public RateLimitDecision getDecision() {
        return decision;
    }
}
