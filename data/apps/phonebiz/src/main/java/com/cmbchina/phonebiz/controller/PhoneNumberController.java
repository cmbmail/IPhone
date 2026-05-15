package com.cmbchina.phonebiz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.PhoneNumber;
import com.cmbchina.phonebiz.service.PhoneNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/phone")
public class PhoneNumberController {

    @Autowired
    private PhoneNumberService phoneNumberService;

    @GetMapping("/page")
    @AuditLog(module = "号码管理", operation = "分页查询号码")
    public Result<Page<PhoneNumber>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) String phoneNumber,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long employeeId) {
        Page<PhoneNumber> page = phoneNumberService.getPhonePage(pageNum, pageSize, phoneNumber, status, employeeId);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "号码管理", operation = "查询号码详情")
    public Result<PhoneNumber> getById(@PathVariable Long id) {
        PhoneNumber phone = phoneNumberService.getPhoneById(id);
        return Result.success(phone);
    }

    @PostMapping
    @AuditLog(module = "号码管理", operation = "新增号码")
    public Result<Void> add(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String remark = request.get("remark");
        boolean success = phoneNumberService.addPhoneNumber(phoneNumber, remark);
        return success ? Result.success() : Result.error("新增失败，号码已存在");
    }

    @PutMapping("/assign/{id}")
    @AuditLog(module = "号码管理", operation = "分配号码")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long employeeId = ((Number) request.get("employeeId")).longValue();
        String employeeName = (String) request.get("employeeName");
        boolean success = phoneNumberService.assignPhone(id, employeeId, employeeName);
        return success ? Result.success() : Result.error("分配失败，状态不允许");
    }

    @PutMapping("/activate/{id}")
    @AuditLog(module = "号码管理", operation = "激活号码")
    public Result<Void> activate(@PathVariable Long id) {
        boolean success = phoneNumberService.activatePhone(id);
        return success ? Result.success() : Result.error("激活失败，状态不允许");
    }

    @PutMapping("/suspend/{id}")
    @AuditLog(module = "号码管理", operation = "停用号码")
    public Result<Void> suspend(@PathVariable Long id) {
        boolean success = phoneNumberService.suspendPhone(id);
        return success ? Result.success() : Result.error("停用失败，状态不允许");
    }

    @PutMapping("/resume/{id}")
    @AuditLog(module = "号码管理", operation = "恢复号码")
    public Result<Void> resume(@PathVariable Long id) {
        boolean success = phoneNumberService.resumePhone(id);
        return success ? Result.success() : Result.error("恢复失败，状态不允许");
    }

    @PutMapping("/recycle/{id}")
    @AuditLog(module = "号码管理", operation = "回收号码")
    public Result<Void> recycle(@PathVariable Long id) {
        boolean success = phoneNumberService.recyclePhone(id);
        return success ? Result.success() : Result.error("回收失败，状态不允许");
    }

    @PutMapping("/reassign/{id}")
    @AuditLog(module = "号码管理", operation = "重新分配号码")
    public Result<Void> reassign(@PathVariable Long id) {
        boolean success = phoneNumberService.reassignPhone(id);
        return success ? Result.success() : Result.error("重新分配失败，状态不允许");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "号码管理", operation = "删除号码")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = phoneNumberService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
