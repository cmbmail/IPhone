package com.cmbchina.phonebiz.controller;

import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.SysMenu;
import com.cmbchina.phonebiz.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/list")
    @AuditLog(module = "菜单管理", operation = "查询菜单列表")
    public Result<List<SysMenu>> list() {
        List<SysMenu> list = sysMenuService.getAllMenus();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "菜单管理", operation = "查询菜单详情")
    public Result<SysMenu> getById(@PathVariable Long id) {
        SysMenu menu = sysMenuService.getMenuById(id);
        return Result.success(menu);
    }

    @PostMapping
    @AuditLog(module = "菜单管理", operation = "新增菜单")
    public Result<Void> add(@RequestBody SysMenu menu) {
        boolean success = sysMenuService.addMenu(menu);
        return success ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    @AuditLog(module = "菜单管理", operation = "修改菜单")
    public Result<Void> update(@RequestBody SysMenu menu) {
        boolean success = sysMenuService.updateMenu(menu);
        return success ? Result.success() : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "菜单管理", operation = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = sysMenuService.deleteMenu(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
