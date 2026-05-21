package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.SubsidiaryReconciliation;
import com.phonebiz.service.SubsidiaryReconciliationService;
import org.springframework.security.core.Authentication;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/reconciliations")
@PreAuthorize("hasAuthority('recon:view') or hasRole('ADMIN') or hasRole('FINANCE')")
@RequiredArgsConstructor
public class SubsidiaryReconciliationController {

    private final SubsidiaryReconciliationService reconciliationService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('recon:view') or hasRole('ADMIN')")
    @AuditLog(module = "reconciliation", operation = "生成对账", targetType = "SubsidiaryReconciliation")
    public ApiResponse<Void> generateReconciliation(@RequestParam String billMonth) {
        reconciliationService.generateReconciliation(billMonth);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<Page<Map<String, Object>>> getReconciliations(
            @RequestParam String billMonth,
            @RequestParam(required = false) Long orgId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Map<String, Object>> page = reconciliationService.getReconciliationsWithOrgName(billMonth, orgId, pageable);
        return ApiResponse.success(page);
    }

    @GetMapping("/{id}")
    public ApiResponse<SubsidiaryReconciliation> getReconciliation(@PathVariable Long id) {
        SubsidiaryReconciliation reconciliation = reconciliationService.getReconciliation(id);
        return ApiResponse.success(reconciliation);
    }

    @GetMapping("/pending")
    public ApiResponse<List<SubsidiaryReconciliation>> getPendingReconciliations(@RequestParam Long orgId) {
        List<SubsidiaryReconciliation> pending = reconciliationService.getPendingReconciliations(orgId);
        return ApiResponse.success(pending);
    }

    @PostMapping("/{id}/subsidiary-confirm")
    @PreAuthorize("hasAuthority('recon:view') or hasRole('ADMIN')")
    @AuditLog(module = "reconciliation", operation = "子公司确认", targetType = "SubsidiaryReconciliation", targetId = "#id")
    public ApiResponse<SubsidiaryReconciliation> subsidiaryConfirm(
            @PathVariable Long id,
            Authentication authentication) {
        SubsidiaryReconciliation reconciliation = reconciliationService.subsidiaryConfirm(id, authentication != null ? authentication.getName() : "system");
        return ApiResponse.success(reconciliation);
    }

    @PostMapping("/{id}/group-confirm")
    @PreAuthorize("hasAuthority('recon:view') or hasRole('ADMIN')")
    @AuditLog(module = "reconciliation", operation = "集团确认", targetType = "SubsidiaryReconciliation", targetId = "#id")
    public ApiResponse<SubsidiaryReconciliation> groupConfirm(
            @PathVariable Long id,
            Authentication authentication) {
        SubsidiaryReconciliation reconciliation = reconciliationService.groupConfirm(id, authentication != null ? authentication.getName() : "system");
        return ApiResponse.success(reconciliation);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary(@RequestParam String billMonth) {
        Map<String, Object> summary = reconciliationService.getReconciliationSummary(billMonth);
        return ApiResponse.success(summary);
    }
}