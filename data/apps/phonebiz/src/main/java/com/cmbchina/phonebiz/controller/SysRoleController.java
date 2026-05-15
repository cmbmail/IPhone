package com.cmbchina.phonebiz.controller;

import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.SysMenu;
import com.cmbchina.phonebiz.entity.SysRole;
import com.cmbchina.phonebiz.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("/list")
    @AuditLog(module = "角色管理", operation = "查询角色列表")
    public Result<List<SysRole>> list() {
        List<SysRole> list = sysRoleService.getAllRoles();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "角色管理", operation = "查询角色详情")
    public Result<SysRole> getById(@PathVariable Long id) {
        SysRole role = sysRoleService.getRoleById(id);
        return Result.success(role);
    }

    @PostMapping
    @AuditLog(module = "角色管理", operation = "新增角色")
    public Result<Void> add(@RequestBody SysRole role) {
        boolean success = sysRoleService.addRole(role);
        return success ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    @AuditLog(module = "角色管理", operation = "修改角色")
    public Result<Void> update(@RequestBody SysRole role) {
        boolean success = sysRoleService.updateRole(role);
        return success ? Result.success() : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "角色管理", operation = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = sysRoleService.deleteRole(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/{id}/menus")
    @AuditLog(module = "角色管理", operation = "查询角色菜单")
    public Result<List<SysMenu>> getRoleMenus(@PathVariable Long id) {
        List<SysMenu> menus = sysRoleService.getMenusByRoleId(id);
        return Result.success(menus);
    }

    @PostMapping("/{id}/menus")
    @AuditLog(module = "角色管理", operation = "分配角色菜单")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        boolean success = sysRoleService.assignMenus(id, menuIds);
        return success ? Result.success() : Result.error("分配失败");
    }
}