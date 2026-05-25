package com.phonebiz.controller;

import java.io.IOException;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.Invoice;
import com.phonebiz.service.InvoiceService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@Slf4j
@RestController
@RequestMapping("/invoices")
@PreAuthorize("hasAuthority('inv:view') or hasRole('ADMIN') or hasRole('FINANCE')")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('inv:create') or hasRole('ADMIN')")
    @AuditLog(module = "invoice", operation = "上传发票", targetType = "Invoice")
    public ApiResponse<Invoice> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam String billMonth,
            @RequestParam Long sourceOrgId,
            Authentication authentication) {
        try {
            Invoice invoice = invoiceService.uploadInvoice(file, billMonth, sourceOrgId, authentication != null ? authentication.getName() : "system");
            return ApiResponse.success(invoice);
        } catch (IOException e) {
            log.error("Invoice upload failed", e);
            return ApiResponse.error(500, "文件上传失败，请稍后重试");
        }
    }

    @PostMapping("/batch-upload")
    @PreAuthorize("hasAuthority('inv:create') or hasRole('ADMIN')")
    @AuditLog(module = "invoice", operation = "批量上传发票", targetType = "Invoice")
    public ApiResponse<Map<String, Object>> batchUploadInvoices(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam String billMonth,
            Authentication authentication) {
        if (files == null || files.length == 0) {
            return ApiResponse.error(400, "请选择至少一个文件");
        }
        Map<String, Object> result = invoiceService.batchUploadInvoices(
                files, billMonth, authentication != null ? authentication.getName() : "system");
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Page<Invoice>> getInvoices(
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orgId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Invoice> result;
        if (orgId != null && status != null) {
            result = invoiceService.getInvoicesByOrgAndStatus(orgId, 
                Integer.parseInt(status), pageable);
        } else if (orgId != null) {
            result = invoiceService.getInvoicesByOrg(orgId, pageable);
        } else if (billMonth != null && status != null) {
            result = invoiceService.getInvoicesByBillMonth(billMonth, pageable);
        } else if (billMonth != null) {
            result = invoiceService.getInvoicesByBillMonth(billMonth, pageable);
        } else if (status != null) {
            result = invoiceService.getInvoicesByStatus(Integer.parseInt(status), pageable);
        } else {
            result = invoiceService.getInvoicesByOrg(null, pageable);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Invoice> getInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('inv:edit') or hasRole('ADMIN')")
    @AuditLog(module = "invoice", operation = "确认发票", targetType = "Invoice", targetId = "#id")
    public ApiResponse<Invoice> confirmInvoice(
            @PathVariable Long id,
            Authentication authentication) {
        Invoice invoice = invoiceService.confirmInvoice(id, authentication != null ? authentication.getName() : "system");
        return ApiResponse.success(invoice);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Invoice> markAsRead(@PathVariable Long id) {
        Invoice invoice = invoiceService.markAsRead(id);
        return ApiResponse.success(invoice);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inv:delete') or hasRole('ADMIN')")
    @AuditLog(module = "invoice", operation = "删除发票", targetType = "Invoice", targetId = "#id")
    public ApiResponse<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Long>> getStatistics(@RequestParam String billMonth) {
        Map<String, Long> stats = invoiceService.getInvoiceStatistics(billMonth);
        return ApiResponse.success(stats);
    }
}