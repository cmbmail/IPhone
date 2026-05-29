package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateRoleRequest;
import com.phonebiz.dto.UpdateRoleRequest;
import com.phonebiz.entity.SysPermission;
import com.phonebiz.entity.SysRole;
import com.phonebiz.service.SysRoleService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/roles")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    public ApiResponse<List<SysRole>> getAllRoles() {
        return ApiResponse.success(roleService.getAllRoles());
    }

    @GetMapping("/active")
    public ApiResponse<List<SysRole>> getActiveRoles() {
        return ApiResponse.success(roleService.getActiveRoles());
    }

    @GetMapping("/{id:[0-9]+}")
    public ApiResponse<SysRole> getRoleById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRoleById(id));
    }

    @GetMapping("/{id:[0-9]+}/permissions")
    public ApiResponse<List<SysPermission>> getRolePermissions(@PathVariable Long id) {
        return ApiResponse.success(roleService.getPermissionsByRoleId(id));
    }

    @GetMapping("/permissions/all")
    public ApiResponse<List<SysPermission>> getAllPermissions() {
        return ApiResponse.success(roleService.getAllPermissions());
    }

    @GetMapping("/permissions/modules")
    public ApiResponse<Map<String, List<SysPermission>>> getPermissionsByModule() {
        return ApiResponse.success(roleService.getPermissionsGroupedByModule());
    }

    @GetMapping("/{id:[0-9]+}/user-count")
    public ApiResponse<Long> getUserCountByRole(@PathVariable Long id) {
        return ApiResponse.success(roleService.getUserCountByRoleId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:role') or hasRole('ADMIN')")
    @AuditLog(module = "role", operation = "创建角色", targetType = "SysRole")
    public ApiResponse<SysRole> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(roleService.createRole(request, operator));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('sys:role') or hasRole('ADMIN')")
    @AuditLog(module = "role", operation = "更新角色", targetType = "SysRole", targetId = "#id")
    public ApiResponse<SysRole> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(roleService.updateRole(id, request, operator));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('sys:role') or hasRole('ADMIN')")
    @AuditLog(module = "role", operation = "删除角色", targetType = "SysRole", targetId = "#id")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success("Role deleted successfully", null);
    }
}
