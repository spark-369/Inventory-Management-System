package com.unique.ims.backend.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies the Redis rate limit to every {@code /api/**} request.
 *
 * <p>Registered as a servlet filter rather than a Spring MVC interceptor so the
 * limit also covers requests that never reach a handler (404s, static lookups),
 * which is exactly the traffic a limiter is meant to absorb. Being a filter it
 * also runs before any controller code, so a throttled request costs no database
 * or cache work.</p>
 *
 * <p>Rejections are written directly as a JSON 429 body (the same shape as
 * {@code ApiErrorResponse}) because the filter sits outside the
 * {@code @RestControllerAdvice} error path.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimiterService rateLimiterService, RateLimitProperties properties) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled() || !isApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitDecision decision = rateLimiterService.check(resolveClientKey(request));
        writeRateLimitHeaders(response, decision);

        if (!decision.allowed()) {
            writeTooManyRequests(response, decision);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Only API traffic is limited; the JobRunr dashboard and static assets are not. */
    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(API_PREFIX);
    }

    /**
     * Resolves the client identity.
     *
     * <p>{@code X-Forwarded-For} is only trusted when
     * {@code app.rate-limit.trust-forwarded-for=true}, because the header is
     * client-controlled: trusting it by default would let a caller bypass the
     * limit by inventing a new address on every request.</p>
     */
    private String resolveClientKey(HttpServletRequest request) {
        if (properties.isTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Left-most entry is the original client.
                int comma = forwarded.indexOf(',');
                String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
                if (!first.isBlank()) {
                    return first.trim();
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private void writeRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (decision.resetEpochSeconds() > 0) {
            response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimitDecision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1L, decision.retryAfterSeconds())));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Limit is %d requests per window; retry in %d second(s)."}"""
                .formatted(decision.limit(), Math.max(1L, decision.retryAfterSeconds())));
    }
}
