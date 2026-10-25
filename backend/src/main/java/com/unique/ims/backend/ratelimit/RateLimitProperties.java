package com.unique.ims.backend.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the Redis-backed rate limiter, bound from
 * {@code app.rate-limit.*} in {@code application.properties}.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Master switch — set to {@code false} to disable limiting entirely. */
    private boolean enabled = true;

    /** Maximum number of requests allowed per client inside one window. */
    private int requestsPerWindow = 120;

    /** Length of the fixed window, in seconds. */
    private int windowSeconds = 60;

    /**
     * When true, the client is identified from {@code X-Forwarded-For} instead of
     * the socket address. Enable only behind a trusted reverse proxy, otherwise a
     * client can spoof the header and bypass the limit.
     */
    private boolean trustForwardedFor = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerWindow() {
        return requestsPerWindow;
    }

    public void setRequestsPerWindow(int requestsPerWindow) {
        this.requestsPerWindow = requestsPerWindow;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public boolean isTrustForwardedFor() {
        return trustForwardedFor;
    }

    public void setTrustForwardedFor(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }
}
