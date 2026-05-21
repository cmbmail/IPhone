package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.phonebiz.common.ApiResponse;
import com.phonebiz.repository.SysRoleRepository;
import com.phonebiz.dto.UserVO;
import com.phonebiz.service.SysUserService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/users")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleRepository roleRepository;

    @GetMapping("/by-org/{orgId}")
    public ApiResponse<List<UserVO>> getUsersByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(sysUserService.getUsersByOrg(orgId));
    }

    @GetMapping
    public ApiResponse<List<UserVO>> getAllUsers() {
        return ApiResponse.success(sysUserService.getAllUsers());
    }

    @PutMapping("/{id}/username")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    public ApiResponse<Void> updateUsername(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.updateUsername(id, body.get("username"), operator);
        return ApiResponse.success("Username updated", null);
    }

    @PutMapping("/{id}/department")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    public ApiResponse<Void> updateDepartment(@PathVariable Long id, @RequestBody Map<String, Long> body, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.updateDepartment(id, body.get("orgId"), operator);
        return ApiResponse.success("Department updated", null);
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    @AuditLog(module = "user", operation = "重置密码", targetType = "SysUser", targetId = "#id")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.resetPassword(id, operator);
        return ApiResponse.success("Password reset", null);
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    @AuditLog(module = "user", operation = "禁用用户", targetType = "SysUser", targetId = "#id")
    public ApiResponse<Void> disableUser(@PathVariable Long id, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.disableUser(id, operator);
        return ApiResponse.success("User disabled", null);
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    @AuditLog(module = "user", operation = "启用用户", targetType = "SysUser", targetId = "#id")
    public ApiResponse<Void> enableUser(@PathVariable Long id, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.enableUser(id, operator);
        return ApiResponse.success("User enabled", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user') or hasRole('ADMIN')")
    @AuditLog(module = "user", operation = "删除用户", targetType = "SysUser", targetId = "#id")
    public ApiResponse<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        sysUserService.deleteUser(id, operator);
        return ApiResponse.success("User deleted", null);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('sys:role') or hasRole('ADMIN')")
    @AuditLog(module = "user", operation = "变更角色", targetType = "SysUser", targetId = "#id")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Long> body, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        Long roleId = body.get("roleId");
        if (roleId == null) throw new IllegalArgumentException("roleId is required");
        // Validate role exists
        roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        sysUserService.updateRole(id, roleId, operator);
        return ApiResponse.success("Role updated", null);
    }
}