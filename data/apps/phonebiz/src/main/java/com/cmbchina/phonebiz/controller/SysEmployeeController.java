package com.cmbchina.phonebiz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.SysEmployee;
import com.cmbchina.phonebiz.entity.SysRole;
import com.cmbchina.phonebiz.service.SysEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
public class SysEmployeeController {

    @Autowired
    private SysEmployeeService sysEmployeeService;

    @GetMapping("/page")
    @AuditLog(module = "员工管理", operation = "查询员工列表")
    public Result<Page<SysEmployee>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String realName,
                                          @RequestParam(required = false) Long orgId) {
        Page<SysEmployee> page = sysEmployeeService.getEmployeePage(pageNum, pageSize, realName, orgId);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "员工管理", operation = "查询员工详情")
    public Result<SysEmployee> getById(@PathVariable Long id) {
        SysEmployee employee = sysEmployeeService.getEmployeeById(id);
        return Result.success(employee);
    }

    @PostMapping
    @AuditLog(module = "员工管理", operation = "新增员工")
    public Result<Void> add(@RequestBody SysEmployee employee) {
        boolean success = sysEmployeeService.addEmployee(employee);
        return success ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    @AuditLog(module = "员工管理", operation = "修改员工")
    public Result<Void> update(@RequestBody SysEmployee employee) {
        boolean success = sysEmployeeService.updateEmployee(employee);
        return success ? Result.success() : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "员工管理", operation = "删除员工")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = sysEmployeeService.deleteEmployee(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/{id}/roles")
    @AuditLog(module = "员工管理", operation = "查询员工角色")
    public Result<List<SysRole>> getEmployeeRoles(@PathVariable Long id) {
        List<SysRole> roles = sysEmployeeService.getRolesByEmployeeId(id);
        return Result.success(roles);
    }

    @PostMapping("/{id}/roles")
    @AuditLog(module = "员工管理", operation = "分配员工角色")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        boolean success = sysEmployeeService.assignRoles(id, roleIds);
        return success ? Result.success() : Result.error("分配失败");
    }
}