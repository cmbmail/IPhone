package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.service.AuthService;
import com.phonebiz.dto.DeleteBillRequest;
import jakarta.validation.Valid;
import com.phonebiz.service.BillImportService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@RestController
@RequestMapping("/bills")
@PreAuthorize("hasAuthority('bill:view') or hasRole('ADMIN') or hasRole('FINANCE')")
@RequiredArgsConstructor
public class BillController {

    private final BillImportService billImportService;
    private final BillRawRepository billRawRepository;
    private final AuthService authService;

    @GetMapping
    public ApiResponse<Page<BillRaw>> getBills(
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) Integer chargeType,
            @PageableDefault(size = 20, sort = "importedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BillRaw> page;
        if (billMonth != null && !billMonth.isEmpty() && chargeType != null) {
            page = billRawRepository.findByBillMonthAndChargeType(billMonth, chargeType, pageable);
        } else if (billMonth != null && !billMonth.isEmpty()) {
            page = billRawRepository.findByBillMonth(billMonth, pageable);
        } else if (chargeType != null) {
            page = billRawRepository.findByChargeType(chargeType, pageable);
        } else {
            page = billRawRepository.findAll(pageable);
        }
        return ApiResponse.success(page);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('bill:import') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "导入账单", targetType = "BillRaw")
    public ApiResponse<Integer> importBills(@RequestParam String billMonth,
                                           @RequestParam("file") MultipartFile file,
                                           Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        try {
            int count = billImportService.importBillRaw(billMonth, file, operator);
            return ApiResponse.success(count);
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Bill import failed", e);
            return ApiResponse.error(500, "账单导入失败，请检查文件格式");
        }
    }

    @PostMapping("/import-and-allocate")
    @PreAuthorize("hasAuthority('bill:import') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "导入并分摊账单", targetType = "BillRaw")
    public ApiResponse<Map<String, Object>> importAndAllocate(@RequestParam String billMonth,
                                             @RequestParam("file") MultipartFile file,
                                             Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        try {
            int count = billImportService.importBillRaw(billMonth, file, operator);
            billImportService.processImportAsync(billMonth, operator);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("billMonth", billMonth);
            result.put("importedCount", count);
            result.put("allocationStatus", "processing");
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Bill import and allocate failed", e);
            return ApiResponse.error(500, "账单导入并分摊失败，请检查文件格式");
        }
    }

    @GetMapping("/allocation-status")
    @PreAuthorize("hasAuthority('bill:view') or hasRole('ADMIN') or hasRole('FINANCE')")
    public ApiResponse<Map<String, Object>> getAllocationStatus(@RequestParam String billMonth) {
        String status = billImportService.getAllocationStatus(billMonth);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("billMonth", billMonth);
        result.put("status", status);
        // Clean up completed/failed status after reading
        if ("completed".equals(status) || status.startsWith("failed")) {
            billImportService.clearAllocationStatus(billMonth);
        }
        return ApiResponse.success(result);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('bill:delete') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "删除账单", targetType = "BillRaw")
    public ApiResponse<Integer> deleteBills(
            @Valid @RequestBody DeleteBillRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        authService.verifyPassword(username, request.getPassword());
        List<BillRaw> bills = billRawRepository.findByBillMonthAndChargeType(request.getBillMonth(), request.getChargeType());
        billRawRepository.deleteAll(bills);
        return ApiResponse.success(bills.size());
    }
}