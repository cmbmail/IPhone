package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.service.SnapshotService;

@RestController
@RequestMapping("/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    // ==================== Months ====================

    @GetMapping("/months")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<List<String>> getAvailableMonths() {
        return ApiResponse.success(snapshotService.getAvailableMonths());
    }

    // ==================== Paged Queries ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Page<PhoneSnapshot>> getSnapshotsPaged(
            @RequestParam String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long branchOrgId) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<PhoneSnapshot> result;
        
        if (branchOrgId != null) {
            result = snapshotService.getSnapshotsByBranchPaged(month, branchOrgId, pageRequest);
        } else if (orgId != null) {
            result = snapshotService.getSnapshotsByOrgPaged(month, orgId, pageRequest);
        } else if (status != null) {
            result = snapshotService.getSnapshotsByStatusPaged(month, status, pageRequest);
        } else {
            result = snapshotService.getSnapshotsPaged(month, pageRequest);
        }
        
        return ApiResponse.success(result);
    }

    // ==================== Stats ====================

    @GetMapping("/{month}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Map<String, Object>> getSnapshotStats(@PathVariable String month) {
        return ApiResponse.success(snapshotService.getSnapshotStats(month));
    }

    @GetMapping("/{month}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Map<String, Object>> getSnapshotCount(@PathVariable String month) {
        int total = snapshotService.getSnapshotCount(month);
        int active = snapshotService.getSnapshotCountByStatus(month, PhoneNumber.PS_ACTIVE);
        int stopped = snapshotService.getSnapshotCountByStatus(month, PhoneNumber.PS_STOPPED);
        int cancelled = snapshotService.getSnapshotCountByStatus(month, PhoneNumber.PS_CANCELLED);
        
        return ApiResponse.success(Map.of(
                "total", total,
                "active", active,
                "stopped", stopped,
                "cancelled", cancelled
        ));
    }

    @GetMapping("/{month}/org/{orgId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Map<String, Object>> getSnapshotCountByOrg(
            @PathVariable String month,
            @PathVariable Long orgId) {
        int count = snapshotService.getSnapshotCountByOrg(month, orgId);
        return ApiResponse.success(Map.of("month", month, "orgId", orgId, "count", count));
    }

    // ==================== Single Snapshot ====================

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<PhoneSnapshot> getSnapshot(@PathVariable Long id) {
        return ApiResponse.success(snapshotService.getSnapshot(id));
    }

    // ==================== Generate / Regenerate ====================

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> triggerSnapshot(@RequestParam String month) {
        snapshotService.triggerSnapshot(month);
        return ApiResponse.success(null);
    }

    @PostMapping("/{month}/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> regenerateSnapshot(@PathVariable String month) {
        snapshotService.regenerateSnapshot(month);
        return ApiResponse.success(null);
    }

    // ==================== Bill Association ====================

    @PostMapping("/link-bill")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ApiResponse<Map<String, Object>> linkToBillMonth(
            @RequestParam String snapshotMonth,
            @RequestParam String billMonth) {
        int count = snapshotService.linkToBillMonth(snapshotMonth, billMonth);
        return ApiResponse.success(Map.of("linkedCount", count, "snapshotMonth", snapshotMonth, "billMonth", billMonth));
    }

    @GetMapping("/by-bill/{billMonth}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<List<PhoneSnapshot>> getSnapshotsByBillMonth(@PathVariable String billMonth) {
        return ApiResponse.success(snapshotService.getSnapshotsByBillMonth(billMonth));
    }
}
