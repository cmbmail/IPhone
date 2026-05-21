package com.phonebiz.security;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationDetails {
    private String username;
    private String role;
    private Long scopeOrgId;
    private Long roleId;
    private List<String> permissions;

    /** Backward-compatible constructor */
    public JwtAuthenticationDetails(String username, String role, Long scopeOrgId) {
        this.username = username;
        this.role = role;
        this.scopeOrgId = scopeOrgId;
        this.roleId = null;
        this.permissions = List.of();
    }
}
