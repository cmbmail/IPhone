package com.cmbchina.phonebiz.controller;

import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.SysOrg;
import com.cmbchina.phonebiz.service.SysOrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/org")
public class SysOrgController {

    @Autowired
    private SysOrgService sysOrgService;

    @GetMapping("/list")
    @AuditLog(module = "组织架构", operation = "查询组织列表")
    public Result<List<SysOrg>> list() {
        List<SysOrg> list = sysOrgService.getAllOrgs();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "组织架构", operation = "查询组织详情")
    public Result<SysOrg> getById(@PathVariable Long id) {
        SysOrg org = sysOrgService.getOrgById(id);
        return Result.success(org);
    }

    @PostMapping
    @AuditLog(module = "组织架构", operation = "新增组织")
    public Result<Void> add(@RequestBody SysOrg org) {
        boolean success = sysOrgService.addOrg(org);
        return success ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    @AuditLog(module = "组织架构", operation = "修改组织")
    public Result<Void> update(@RequestBody SysOrg org) {
        boolean success = sysOrgService.updateOrg(org);
        return success ? Result.success() : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "组织架构", operation = "删除组织")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = sysOrgService.deleteOrg(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
