package com.phonebiz.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationDetails {
    private String username;
    private String role;
    private Long scopeOrgId;
}
