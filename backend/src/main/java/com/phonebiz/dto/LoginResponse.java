package com.phonebiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.phonebiz.entity.SysUser;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private long expiresIn;
    private boolean forceChangePassword;
    private UserInfo user;
    private List<String> permissions;

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
        private List<String> permissions;
    }

    private static String mapRoleToName(int role) {
        return switch (role) {
            case 1 -> "admin";
            case 2 -> "ops";
            case 3 -> "finance";
            case 4 -> "boss";
            default -> "user";
        };
    }

    public static LoginResponse from(SysUser user, String token, long expiresIn, List<String> permissions) {
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .permissions(permissions)
                .user(userToInfo(user, permissions))
                .build();
    }

    public static UserInfo userToInfo(SysUser user, List<String> permissions) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .employeeNo(user.getEmployeeNo())
                .role(mapRoleToName(user.getRole()))
                .scopeOrgId(user.getScopeOrgId())
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .needsPasswordChange(user.needsPasswordChange())
                .permissions(permissions)
                .build();
    }
}
