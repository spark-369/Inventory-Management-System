package com.unique.ims.backend.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the servlet filter that enforces the rate limit.
 *
 * <p>The limiter collaborator is stubbed with Mockito, which keeps this a real
 * unit test: no Redis, no Spring context, and the assertions focus purely on
 * HTTP behaviour — status codes, headers and client-key resolution.</p>
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setRequestsPerWindow(10);
        properties.setWindowSeconds(60);
        properties.setTrustForwardedFor(false);
        filter = new RateLimitFilter(rateLimiterService, properties);
    }

    private static RateLimitDecision allowed() {
        return new RateLimitDecision(true, 10, 9, 55, 1_700_000_000L);
    }

    private static RateLimitDecision rejected() {
        return new RateLimitDecision(false, 10, 0, 42, 1_700_000_000L);
    }

    @Test
    @DisplayName("lets an allowed API request through and advertises the quota headers")
    void allowsApiRequestAndSetsHeaders() throws Exception {
        when(rateLimiterService.check(any())).thenReturn(allowed());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1700000000");
    }

    @Test
    @DisplayName("returns 429 with Retry-After and does not continue the chain")
    void rejectsApiRequestWithTooManyRequests() throws Exception {
        when(rateLimiterService.check(any())).thenReturn(rejected());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sales");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
        // The whole point: a throttled request must never reach the controller.
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("ignores non-API paths entirely")
    void ignoresNonApiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).check(any());
    }

    @Test
    @DisplayName("skips the limiter completely when disabled")
    void skipsWhenDisabled() throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).check(any());
    }

    @Test
    @DisplayName("keys on the socket address when X-Forwarded-For is not trusted")
    void usesRemoteAddressByDefault() throws Exception {
        when(rateLimiterService.check(any())).thenReturn(allowed());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("203.0.113.9");
        // A spoofed header must be ignored while trustForwardedFor is false.
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("203.0.113.9");
    }

    @Test
    @DisplayName("keys on the left-most forwarded address when the header is trusted")
    void usesForwardedForWhenTrusted() throws Exception {
        properties.setTrustForwardedFor(true);
        when(rateLimiterService.check(any())).thenReturn(allowed());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.1");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("198.51.100.7");
    }

    @Test
    @DisplayName("falls back to X-Real-IP when X-Forwarded-For is absent")
    void usesRealIpAsSecondChoice() throws Exception {
        properties.setTrustForwardedFor(true);
        when(rateLimiterService.check(any())).thenReturn(allowed());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "192.0.2.55");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiterService).check("192.0.2.55");
    }

    @Test
    @DisplayName("clamps Retry-After to at least one second")
    void retryAfterIsNeverZero() throws Exception {
        when(rateLimiterService.check(any())).thenReturn(new RateLimitDecision(false, 10, 0, 0, 0L));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    }
}
