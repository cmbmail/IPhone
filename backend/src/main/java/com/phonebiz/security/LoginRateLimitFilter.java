package com.phonebiz.security;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    @Value("${phonebiz.rate-limit.max-attempts:10}")
    private int maxAttempts;

    @Value("${phonebiz.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, AttemptTracker> attemptMap = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Maximum entries in the rate limit map to prevent memory leak */
    private static final int MAX_ENTRIES = 10_000;

    /** Cleanup interval in seconds */
    private static final long CLEANUP_INTERVAL_SECONDS = 300;
    private volatile long lastCleanupTime = Instant.now().getEpochSecond();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        // Only rate-limit login requests
        if (!requestUri.endsWith("/auth/login") || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // M-04: Periodic cleanup to prevent memory leak
        long cleanupNow = Instant.now().getEpochSecond();
        if (cleanupNow - lastCleanupTime > CLEANUP_INTERVAL_SECONDS) {
            cleanupExpiredEntries(cleanupNow);
            lastCleanupTime = cleanupNow;
        }

        String clientIp = getClientIp(request);
        String key = "login:" + clientIp;

        AttemptTracker tracker = attemptMap.computeIfAbsent(key, k -> new AttemptTracker());

        long now = Instant.now().getEpochSecond();

        synchronized (tracker) {
            if (now - tracker.windowStart > windowSeconds) {
                tracker.windowStart = now;
                tracker.count.set(0);
            }

            if (tracker.count.incrementAndGet() > maxAttempts) {
                log.warn("Login rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    objectMapper.writeValueAsString(java.util.Map.of(
                        "code", 429,
                        "message", "Too many login attempts. Please try again later.",
                        "data", java.util.Map.of()
                    ))
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Only trust X-Real-IP from our known nginx proxy (127.0.0.1)
        // Never trust X-Forwarded-For from external clients
        String remoteAddr = request.getRemoteAddr();
        if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private static class AttemptTracker {
        long windowStart = Instant.now().getEpochSecond();
        AtomicInteger count = new AtomicInteger(0);
    }

    private void cleanupExpiredEntries(long now) {
        attemptMap.entrySet().removeIf(entry -> {
            AttemptTracker tracker = entry.getValue();
            synchronized (tracker) {
                return now - tracker.windowStart > windowSeconds * 2;
            }
        });
        // Also limit total entries
        if (attemptMap.size() > MAX_ENTRIES) {
            attemptMap.clear();
            log.info("Rate limit map cleared due to size exceeding {}", MAX_ENTRIES);
        }
    }
}
