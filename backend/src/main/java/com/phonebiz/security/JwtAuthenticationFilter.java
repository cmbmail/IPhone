package com.phonebiz.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.entity.SysUser;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserRepository sysUserRepository;

    /** Endpoints allowed even when user must change password */
    private static final Set<String> FORCE_CHANGE_PW_ALLOWED = Set.of(
            "/auth/change-password", "/auth/me", "/auth/health", "/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            Long scopeOrgId = jwtUtil.getScopeOrgIdFromToken(token);
            Long roleId = jwtUtil.getRoleIdFromToken(token);
            List<String> permissions = jwtUtil.getPermissionsFromToken(token);

            if (username != null) {
                // Build authorities from permissions list
                List<SimpleGrantedAuthority> authorities;
                if (permissions != null && !permissions.isEmpty()) {
                    authorities = permissions.stream()
                            .map(p -> new SimpleGrantedAuthority(p))
                            .collect(Collectors.toList());
                    // Also add ROLE_ prefix for role-based checks
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + mapRoleToName(role)));
                    }
                } else {
                    // Fallback: role-only authority for backward compatibility
                    authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + (role != null ? mapRoleToName(role) : "USER"))
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                authentication.setDetails(new JwtAuthenticationDetails(username, role, scopeOrgId, roleId, permissions));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // S11: Force password change check
                if (mustChangePassword(username)) {
                    String requestUri = request.getRequestURI();
                    // Strip context-path prefix (/api)
                    String path = requestUri.replace("/api", "");
                    if (!FORCE_CHANGE_PW_ALLOWED.contains(path)) {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                            "{\"code\":1007,\"message\":\"Password change required\",\"data\":null}"
                        );
                        return;
                    }
                }

                // M-02: Invalidate tokens issued before password change
                if (isTokenInvalidatedByPasswordChange(token, username)) {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"code\":10401,\"message\":\"Token invalidated by password change\",\"data\":null}"
                    );
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenInvalidatedByPasswordChange(String token, String username) {
        try {
            var claims = jwtUtil.parseClaims(token);
            var issuedAt = claims.getIssuedAt();
            if (issuedAt == null) return false;
            var userOpt = sysUserRepository.findByUsername(username);
            if (userOpt.isEmpty()) return false;
            SysUser user = userOpt.get();
            var pwdChangedAt = user.getPasswordChangedAt();
            if (pwdChangedAt == null) return false;
            // If token was issued before password change, it's invalid
            return issuedAt.toInstant().isBefore(pwdChangedAt.atZone(java.time.ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.debug("Token password-change check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean mustChangePassword(String username) {
        try {
            return sysUserRepository.findByUsername(username)
                    .map(u -> u.needsPasswordChange())
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check password change status");
            return false;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String mapRoleToName(String role) {
        try {
            int roleCode = Integer.parseInt(role);
            return switch (roleCode) {
                case 1 -> "ADMIN";
                case 2 -> "OPS";
                case 3 -> "FINANCE";
                case 4 -> "BOSS";
                default -> "USER";
            };
        } catch (NumberFormatException e) {
            return role.toUpperCase();
        }
    }
}
