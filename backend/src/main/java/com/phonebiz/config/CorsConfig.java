package com.phonebiz.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${phonebiz.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Only allow explicitly configured origins; no wildcard
        if (allowedOrigins == null || allowedOrigins.isEmpty() 
                || (allowedOrigins.size() == 1 && "*".equals(allowedOrigins.get(0)))) {
            throw new IllegalStateException(
                "phonebiz.cors.allowed-origins must be explicitly configured; wildcard '*' is not allowed");
        }
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        // allowCredentials is safe only because allowedOrigins are explicit (not wildcard)
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
