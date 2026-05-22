package com.phonebiz.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for login endpoint.
 * Limits each IP to maxAttempts login attempts per windowSeconds.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    @Value("${phonebiz.rate-limit.max-attempts:10}")
    private int maxAttempts;

    @Value("${phonebiz.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final Map<String, AttemptTracker> trackers = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("/api/auth/login".equals(httpRequest.getRequestURI())
                && "POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String clientIp = getClientIp(httpRequest);
            if (isRateLimited(clientIp)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too many login attempts, please try again later\"}");
                return;
            }
            recordAttempt(clientIp);
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        AttemptTracker tracker = trackers.get(clientIp);
        if (tracker == null) return false;
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);
        return tracker.countSince(windowStart) >= maxAttempts;
    }

    private void recordAttempt(String clientIp) {
        trackers.computeIfAbsent(clientIp, k -> new AttemptTracker()).record();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public void destroy() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds * 2L);
        trackers.entrySet().removeIf(e -> e.getValue().isStale(cutoff));
    }

    private static class AttemptTracker {
        private final ConcurrentHashMap<Long, Boolean> attempts = new ConcurrentHashMap<>();

        void record() {
            attempts.put(Instant.now().toEpochMilli(), true);
        }

        int countSince(Instant windowStart) {
            long cutoff = windowStart.toEpochMilli();
            attempts.entrySet().removeIf(e -> e.getKey() < cutoff);
            return attempts.size();
        }

        boolean isStale(Instant cutoff) {
            attempts.entrySet().removeIf(e -> e.getKey() < cutoff.toEpochMilli());
            return attempts.isEmpty();
        }
    }
}
