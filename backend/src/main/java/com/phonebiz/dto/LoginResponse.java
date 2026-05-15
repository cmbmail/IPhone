package com.phonebiz.dto;

import com.phonebiz.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String employeeNo;
        private String role;
        private Long scopeOrgId;
        private String lastLoginAt;
        private Boolean needsPasswordChange;
    }

    public static LoginResponse from(SysUser user, String token, long expiresIn) {
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .user(UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .employeeNo(user.getEmployeeNo())
                        .role(user.getRole().name())
                        .scopeOrgId(user.getScopeOrgId())
                        .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                        .needsPasswordChange(user.needsPasswordChange())
                        .build())
                .build();
    }
}
