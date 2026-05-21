package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.service.AuthService;
import com.phonebiz.dto.DeleteBillRequest;
import jakarta.validation.Valid;
import com.phonebiz.service.BillImportService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

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
            @RequestParam(required = false) String chargeType,
            @PageableDefault(size = 20, sort = "importedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BillRaw> page;
        if (billMonth != null && !billMonth.isEmpty() && chargeType != null && !chargeType.isEmpty()) {
            page = billRawRepository.findByBillMonthAndChargeType(billMonth, chargeType, pageable);
        } else if (billMonth != null && !billMonth.isEmpty()) {
            page = billRawRepository.findByBillMonth(billMonth, pageable);
        } else if (chargeType != null && !chargeType.isEmpty()) {
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
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PostMapping("/import-and-allocate")
    @PreAuthorize("hasAuthority('bill:import') or hasRole('ADMIN')")
    @AuditLog(module = "bill", operation = "导入并分摊账单", targetType = "BillRaw")
    public ApiResponse<Void> importAndAllocate(@RequestParam String billMonth,
                                             @RequestParam("file") MultipartFile file,
                                             Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        try {
            billImportService.importBillRaw(billMonth, file, operator);
            billImportService.processImportAsync(billMonth, operator);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
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