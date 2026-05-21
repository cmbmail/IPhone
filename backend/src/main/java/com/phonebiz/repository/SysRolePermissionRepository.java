package com.phonebiz.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SysRolePermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Long> findPermissionIdsByRoleId(Long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT permission_id FROM sys_role_permission WHERE role_id = ?",
                Long.class, roleId);
    }

    public void savePermissions(Long roleId, List<Long> permissionIds) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                    permissionIds.stream()
                            .map(pid -> new Object[]{roleId, pid})
                            .toList());
        }
    }

    public void deleteByRoleId(Long roleId) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
    }
}
