package com.phonebiz.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adds security-related HTTP response headers.
 * Registered as a global servlet filter (before Spring Security filter chain)
 * to ensure headers are not overridden by Spring Security's HeaderWriterFilter.
 */
@Component
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        chain.doFilter(request, response);

        // Re-apply after chain to override any Spring Security overrides
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }
}
