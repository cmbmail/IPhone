package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.BillAllocationSummaryDTO;
import java.math.BigDecimal;
import com.phonebiz.repository.BillRawRepository;
import java.util.ArrayList;
import java.util.List;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.repository.BillAllocationRepository;
import com.phonebiz.service.BillAllocationService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/bill-allocations")
@PreAuthorize("hasAuthority('bill:view') or hasRole('ADMIN') or hasRole('FINANCE')")
@RequiredArgsConstructor
public class BillAllocationController {

    private final BillAllocationRepository billAllocationRepository;
    private final BillAllocationService billAllocationService;
    private final BillRawRepository billRawRepository;

    @GetMapping
    public ApiResponse<Page<BillAllocation>> getAllocations(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonth(billMonth, pageable));
    }

    @GetMapping("/anomalies")
    public ApiResponse<Page<BillAllocation>> getAnomalies(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonthAndAnomalyFlag(billMonth, true, pageable));
    }

    @GetMapping("/pending-org-confirm")
    public ApiResponse<Page<BillAllocation>> getPendingOrgConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonthAndAdminConfirmOrg(billMonth, BillAllocation.ConfirmStatus.pending, pageable));
    }

    @GetMapping("/pending-amount-confirm")
    public ApiResponse<Page<BillAllocation>> getPendingAmountConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonthAndAdminConfirmAmount(billMonth, BillAllocation.ConfirmStatus.pending, pageable));
    }

    @GetMapping("/pending-finance-confirm")
    public ApiResponse<Page<BillAllocation>> getPendingFinanceConfirm(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonthAndFinanceConfirmAnomaly(billMonth, BillAllocation.FinanceConfirmStatus.pending, pageable));
    }

    @GetMapping("/pending-submit")
    public ApiResponse<Page<BillAllocation>> getPendingSubmit(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(billAllocationRepository.findByBillMonthAndFinanceConfirmSubmit(billMonth, BillAllocation.FinanceSubmitStatus.pending, pageable));
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
    public ApiResponse<java.util.List<BillAllocationSummaryDTO>> getAllocationSummary(
            @RequestParam String billMonth) {
        List<Object[]> rows = billRawRepository.sumByBranch(billMonth);
        List<BillAllocationSummaryDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String branchName = (String) row[0];
            if (branchName == null || branchName.isEmpty()) {
                branchName = "未分配";
            }
            BigDecimal platformUsageFee = (BigDecimal) row[1];
            BigDecimal numberMonthlyRent = (BigDecimal) row[2];
            Integer outboundDuration = ((Number) row[3]).intValue();
            Integer transferOutboundDuration = ((Number) row[4]).intValue();
            BigDecimal domesticCharge = (BigDecimal) row[5];
            BigDecimal internationalCharge = (BigDecimal) row[6];
            BigDecimal recordingFee = (BigDecimal) row[7];
            BigDecimal ringtoneFee = (BigDecimal) row[8];
            BigDecimal flashSmsFee = (BigDecimal) row[9];
            BigDecimal totalAmount = (BigDecimal) row[10];
            BigDecimal feeSubtotal = platformUsageFee.add(numberMonthlyRent).add(domesticCharge).add(internationalCharge);

            result.add(BillAllocationSummaryDTO.builder()
                    .branchName(branchName)
                    .platformUsageFee(platformUsageFee)
                    .numberMonthlyRent(numberMonthlyRent)
                    .outboundDuration(outboundDuration)
                    .transferOutboundDuration(transferOutboundDuration)
                    .domesticCharge(domesticCharge)
                    .internationalCharge(internationalCharge)
                    .feeSubtotal(feeSubtotal)
                    .recordingFee(recordingFee)
                    .ringtoneFee(ringtoneFee)
                    .flashSmsFee(flashSmsFee)
                    .totalAmount(totalAmount)
                    .build());
        }
        return ApiResponse.success(result);
    }

}