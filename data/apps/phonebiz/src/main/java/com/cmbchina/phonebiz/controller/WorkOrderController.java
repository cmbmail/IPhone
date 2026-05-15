package com.cmbchina.phonebiz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.entity.WorkOrder;
import com.cmbchina.phonebiz.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping("/page")
    @AuditLog(module = "工单管理", operation = "分页查询工单")
    public Result<Page<WorkOrder>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String orderNo,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long requesterId,
                                        @RequestParam(required = false) Long handlerId,
                                        @RequestParam(required = false) String orderType) {
        Page<WorkOrder> page = workOrderService.getOrderPage(pageNum, pageSize, orderNo, status, requesterId, handlerId, orderType);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @AuditLog(module = "工单管理", operation = "查询工单详情")
    public Result<WorkOrder> getById(@PathVariable Long id) {
        WorkOrder order = workOrderService.getOrderById(id);
        return Result.success(order);
    }

    @PostMapping
    @AuditLog(module = "工单管理", operation = "创建工单")
    public Result<Void> create(@RequestBody WorkOrder order) {
        boolean success = workOrderService.createOrder(order);
        return success ? Result.success() : Result.error("创建失败");
    }

    @PutMapping("/accept/{id}")
    @AuditLog(module = "工单管理", operation = "受理工单")
    public Result<Void> accept(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> request) {
        Long handlerId = null;
        String handlerName = null;
        if (request != null) {
            if (request.get("handlerId") != null) {
                handlerId = ((Number) request.get("handlerId")).longValue();
            }
            handlerName = (String) request.get("handlerName");
        }
        boolean success = workOrderService.acceptOrder(id, handlerId, handlerName);
        return success ? Result.success() : Result.error("受理失败，状态不允许");
    }

    @PutMapping("/approve/{id}")
    @AuditLog(module = "工单管理", operation = "审批工单")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String result = request.get("result");
        boolean success = workOrderService.approveOrder(id, result);
        return success ? Result.success() : Result.error("审批失败，状态不允许");
    }

    @PutMapping("/reject/{id}")
    @AuditLog(module = "工单管理", operation = "拒绝工单")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        boolean success = workOrderService.rejectOrder(id, reason);
        return success ? Result.success() : Result.error("拒绝失败，状态不允许");
    }

    @PutMapping("/complete/{id}")
    @AuditLog(module = "工单管理", operation = "完成工单")
    public Result<Void> complete(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String result = request.get("result");
        boolean success = workOrderService.completeOrder(id, result);
        return success ? Result.success() : Result.error("完成失败，状态不允许");
    }

    @PutMapping("/cancel/{id}")
    @AuditLog(module = "工单管理", operation = "取消工单")
    public Result<Void> cancel(@PathVariable Long id) {
        boolean success = workOrderService.cancelOrder(id);
        return success ? Result.success() : Result.error("取消失败，状态不允许");
    }

    @PutMapping("/reopen/{id}")
    @AuditLog(module = "工单管理", operation = "重新打开工单")
    public Result<Void> reopen(@PathVariable Long id) {
        boolean success = workOrderService.reopenOrder(id);
        return success ? Result.success() : Result.error("重新打开失败，状态不允许");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "工单管理", operation = "删除工单")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = workOrderService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
