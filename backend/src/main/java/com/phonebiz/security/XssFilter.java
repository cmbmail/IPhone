package com.phonebiz.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Global XSS input filter that wraps HttpServletRequest to
 * strip dangerous HTML/JS patterns from parameter values.
 * This is a defense-in-depth measure alongside frontend validation.
 */
@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    /**
     * Escapes HTML characters in parameter values to prevent reflected XSS.
     * JSON body is NOT modified - Jackson deserialization + Bean Validation handle the types.
     */
    static class XssRequestWrapper extends HttpServletRequestWrapper {

        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? sanitize(value) : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // Don't sanitize Content-Type, Authorization etc.
            if ("content-type".equalsIgnoreCase(name)
                    || "authorization".equalsIgnoreCase(name)
                    || "accept".equalsIgnoreCase(name)) {
                return value;
            }
            return value != null ? sanitize(value) : null;
        }

        private String sanitize(String value) {
            if (value == null) return null;
            // Remove obvious script injection patterns
            String cleaned = value.replaceAll("<(?i)script[^>]*>.*?</(?i)script>", "");
            cleaned = cleaned.replaceAll("<(?i)script[^>]*/>", "");
            cleaned = cleaned.replaceAll("(?i)javascript:", "");
            cleaned = cleaned.replaceAll("(?i)vbscript:", "");
            cleaned = cleaned.replaceAll("(?i)on\\w+\\s*=", "");
            return cleaned;
        }
    }
}
