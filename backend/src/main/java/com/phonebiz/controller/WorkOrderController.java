package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateWorkOrderRequest;
import com.phonebiz.dto.UpdateWorkOrderRequest;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.dto.WorkOrderItemDTO;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.service.WorkOrderService;
import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.entity.SysUser;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/work-orders")
@PreAuthorize("hasAuthority('wo:view') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final SysUserRepository sysUserRepository;

    @GetMapping
    public ApiResponse<Page<WorkOrderDTO>> getWorkOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkOrderDTO> result;
        if (status != null) {
            Integer workOrderStatus = Integer.parseInt(status);
            result = workOrderService.getWorkOrdersByStatus(workOrderStatus, pageable);
        } else {
            result = workOrderService.getWorkOrders(pageable);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderDTO> getWorkOrderById(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.getWorkOrderById(id));
    }

    @GetMapping("/no/{workOrderNo}")
    public ApiResponse<WorkOrderDTO> getWorkOrderByNo(@PathVariable String workOrderNo) {
        return ApiResponse.success(workOrderService.getWorkOrderByNo(workOrderNo));
    }

    @GetMapping("/requester/{requesterId}")
    public ApiResponse<Page<WorkOrderDTO>> getWorkOrdersByRequester(
            @PathVariable Long requesterId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(workOrderService.getWorkOrdersByRequester(requesterId, pageable));
    }

    @GetMapping("/handler/{handlerId}")
    public ApiResponse<Page<WorkOrderDTO>> getWorkOrdersByHandler(
            @PathVariable Long handlerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(workOrderService.getWorkOrdersByHandler(handlerId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN')")
    @AuditLog(module = "work-order", operation = "创建工单", targetType = "WorkOrder")
    public ApiResponse<WorkOrderDTO> createWorkOrder(@Valid @RequestBody CreateWorkOrderRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        Long requesterId = sysUserRepository.findByUsername(username)
                .map(SysUser::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004));
        WorkOrderDTO result = workOrderService.createWorkOrder(request, requesterId, username);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN')")
    @AuditLog(module = "work-order", operation = "更新工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<WorkOrderDTO> updateWorkOrder(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateWorkOrderRequest request) {
        return ApiResponse.success(workOrderService.updateWorkOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('wo:delete') or hasRole('ADMIN')")
    @AuditLog(module = "work-order", operation = "删除工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<Void> deleteWorkOrder(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ApiResponse.success("Work order deleted", null);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN') or hasRole('OPS')")
    @AuditLog(module = "work-order", operation = "接单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<WorkOrderDTO> acceptWorkOrder(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        Long handlerId = sysUserRepository.findByUsername(username)
                .map(SysUser::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004));
        return ApiResponse.success(workOrderService.acceptWorkOrder(id, handlerId, username));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN') or hasRole('OPS')")
    @AuditLog(module = "work-order", operation = "处理工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<WorkOrderDTO> processWorkOrder(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.processWorkOrder(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN') or hasRole('OPS')")
    @AuditLog(module = "work-order", operation = "完成工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<WorkOrderDTO> completeWorkOrder(@PathVariable Long id,
                                                      @RequestParam(required = false) String remark) {
        return ApiResponse.success(workOrderService.completeWorkOrder(id, remark));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN')")
    @AuditLog(module = "work-order", operation = "驳回工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<WorkOrderDTO> rejectWorkOrder(@PathVariable Long id,
                                                    @RequestParam String reason) {
        return ApiResponse.success(workOrderService.rejectWorkOrder(id, reason));
    }

    @PostMapping("/{id}/batch-split")
    @PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN')")
    @AuditLog(module = "work-order", operation = "拆分工单", targetType = "WorkOrder", targetId = "#id")
    public ApiResponse<List<WorkOrderDTO>> batchSplitWorkOrder(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.batchSplitWorkOrder(id));
    }

    @PostMapping("/items/{itemId}/execute")
    @PreAuthorize("hasAuthority('wo:edit') or hasRole('ADMIN') or hasRole('OPS')")
    public ApiResponse<WorkOrderItemDTO> executeWorkOrderItem(@PathVariable Long itemId) {
        return ApiResponse.success(workOrderService.executeWorkOrderItem(itemId));
    }
}
