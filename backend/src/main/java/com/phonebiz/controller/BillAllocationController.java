package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.BillAllocationDTO;
import com.phonebiz.dto.BillAllocationSummaryDTO;
import com.phonebiz.dto.BranchAllocationDTO;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.service.BillAllocationService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/bill-allocations")
@PreAuthorize("hasAuthority('bill:view') or hasRole('ADMIN') or hasRole('FINANCE')")
@RequiredArgsConstructor
public class BillAllocationController {

    private final BillAllocationService billAllocationService;

    @GetMapping
    public ApiResponse<Page<BillAllocationDTO>> getAllocations(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getAllocations(billMonth, pageable));
    }

    @GetMapping("/anomalies")
    public ApiResponse<Page<BillAllocationDTO>> getAnomalies(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getAnomalies(billMonth, pageable));
    }

    @GetMapping("/pending-org-confirm")
    public ApiResponse<Page<BillAllocationDTO>> getPendingOrgConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getPendingOrgConfirm(billMonth, pageable));
    }

    @GetMapping("/pending-amount-confirm")
    public ApiResponse<Page<BillAllocationDTO>> getPendingAmountConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getPendingAmountConfirm(billMonth, pageable));
    }

    @GetMapping("/pending-finance-confirm")
    public ApiResponse<Page<BillAllocationDTO>> getPendingFinanceConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getPendingFinanceConfirm(billMonth, pageable));
    }

    @GetMapping("/pending-submit")
    public ApiResponse<Page<BillAllocationDTO>> getPendingSubmit(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationService.getPendingSubmit(billMonth, pageable));
    }

    @PostMapping("/{id:[0-9]+}/confirm-org")
    @PreAuthorize("hasAuthority('bill:allocate') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "确认组织", targetType = "BillAllocation", targetId = "#id")
    public ApiResponse<Void> confirmOrg(@PathVariable Long id,
                                       @RequestParam String status,
                                       @RequestParam(required = false, defaultValue = "admin") String confirmBy) {
        billAllocationService.adminConfirmOrg(id, com.phonebiz.common.EnumHelper.parse(BillAllocation.ConfirmStatus.class, status), confirmBy);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id:[0-9]+}/confirm-amount")
    @PreAuthorize("hasAuthority('bill:allocate') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "确认金额", targetType = "BillAllocation", targetId = "#id")
    public ApiResponse<Void> confirmAmount(@PathVariable Long id,
                                          @RequestParam String status,
                                          @RequestParam(required = false, defaultValue = "admin") String confirmBy) {
        billAllocationService.adminConfirmAmount(id, com.phonebiz.common.EnumHelper.parse(BillAllocation.ConfirmStatus.class, status), confirmBy);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id:[0-9]+}/confirm-anomaly")
    @PreAuthorize("hasAuthority('bill:allocate') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "确认异常", targetType = "BillAllocation", targetId = "#id")
    public ApiResponse<Void> confirmAnomaly(@PathVariable Long id,
                                           @RequestParam String status,
                                           @RequestParam(required = false, defaultValue = "admin") String confirmBy) {
        billAllocationService.financeConfirmAnomaly(id, com.phonebiz.common.EnumHelper.parse(BillAllocation.FinanceConfirmStatus.class, status), confirmBy);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id:[0-9]+}/submit")
    @PreAuthorize("hasAuthority('bill:allocate') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "提交账单", targetType = "BillAllocation", targetId = "#id")
    public ApiResponse<Void> submit(@PathVariable Long id,
                                  Authentication authentication) {
        String submitBy = authentication != null ? authentication.getName() : "system";
        billAllocationService.financeSubmit(id, submitBy);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id:[0-9]+}/reject")
    @PreAuthorize("hasAuthority('bill:allocate') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "驳回账单", targetType = "BillAllocation", targetId = "#id")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                   @RequestParam(required = false) String reason) {
        billAllocationService.rejectAndReset(id, reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/allocation-summary")
    public ApiResponse<List<BillAllocationSummaryDTO>> getAllocationSummary(
            @RequestParam String billMonth) {
        return ApiResponse.success(billAllocationService.getAllocationSummary(billMonth));
    }

    @GetMapping("/branch-allocation")
    public ApiResponse<BranchAllocationDTO.BranchAllocationResponse> getBranchAllocation(
            @RequestParam String billMonth) {
        return ApiResponse.success(billAllocationService.getBranchAllocation(billMonth));
    }

}