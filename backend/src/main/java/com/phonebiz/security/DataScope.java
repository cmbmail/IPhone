package com.phonebiz.security;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.phonebiz.security.JwtAuthenticationDetails;

/**
 * Utility component for data scope isolation based on user's scopeOrgId.
 */
@Slf4j
@Component
public class DataScope {

    /**
     * Get the current user's scopeOrgId from JWT.
     * Returns null if user has no scope restriction (admin).
     */
    public Long getCurrentScopeOrgId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtAuthenticationDetails details) {
                return details.getScopeOrgId();
            }
        } catch (Exception e) {
            log.warn("Failed to get scopeOrgId: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current user's role from JWT.
     */
    public String getCurrentRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtAuthenticationDetails details) {
                return details.getRole();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Check if current user is admin (no data scope restriction).
     */
    public boolean isAdmin() {
        String role = getCurrentRole();
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Check if a given orgId is within the current user's data scope.
     * Admin users can access all orgs.
     */
    public boolean isWithinScope(Long orgId, List<Long> accessibleOrgIds) {
        if (isAdmin()) return true;
        if (orgId == null) return false;
        return accessibleOrgIds != null && accessibleOrgIds.contains(orgId);
    }
}
