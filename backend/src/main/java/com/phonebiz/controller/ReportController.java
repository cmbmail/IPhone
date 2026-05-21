package com.phonebiz.controller;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAuthority('rpt:view') or hasRole('ADMIN') or hasRole('FINANCE') or hasRole('BOSS')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/cost-summary")
    public ApiResponse<Map<String, Object>> getCostSummary(@RequestParam String billMonth) {
        Map<String, Object> summary = reportService.getBillAllocationReport(billMonth);
        return ApiResponse.success(summary);
    }

    @GetMapping("/phone-asset")
    public ApiResponse<Map<String, Object>> getPhoneAssetReport(@RequestParam String billMonth) {
        Map<String, Object> report = reportService.getPhoneAssetReport(billMonth);
        return ApiResponse.success(report);
    }

    @GetMapping("/bill-allocation")
    public ApiResponse<Map<String, Object>> getBillAllocationReport(@RequestParam String billMonth) {
        Map<String, Object> report = reportService.getBillAllocationReport(billMonth);
        return ApiResponse.success(report);
    }

    @GetMapping("/work-order")
    public ApiResponse<Map<String, Object>> getWorkOrderReport(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        Map<String, Object> report = reportService.getWorkOrderReport(startTime, endTime);
        return ApiResponse.success(report);
    }

    @GetMapping("/anomaly-bill")
    public ApiResponse<Map<String, Object>> getAnomalyBillReport(@RequestParam String billMonth) {
        Map<String, Object> report = reportService.getAnomalyBillReport(billMonth);
        return ApiResponse.success(report);
    }

    @GetMapping("/anomaly-bill/page")
    public ApiResponse<Page<BillAllocation>> getAnomalyBillPage(
            @RequestParam String billMonth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BillAllocation> page = reportService.getAnomalyBillPage(billMonth, pageable);
        return ApiResponse.success(page);
    }
}