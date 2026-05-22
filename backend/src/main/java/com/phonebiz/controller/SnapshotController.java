package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

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

    @GetMapping("/months")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<List<String>> getAvailableMonths() {
        List<String> months = snapshotService.getAvailableMonths();
        return ApiResponse.success(months);
    }

    @GetMapping("/month/{month}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<List<PhoneSnapshot>> getSnapshotsByMonth(@PathVariable String month) {
        List<PhoneSnapshot> snapshots = snapshotService.getSnapshotsByMonth(month);
        return ApiResponse.success(snapshots);
    }

    @GetMapping("/{month}/org/{orgId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<List<PhoneSnapshot>> getSnapshotsByMonthAndOrg(
            @PathVariable String month,
            @PathVariable Long orgId) {
        List<PhoneSnapshot> snapshots = snapshotService.getSnapshotsByMonthAndOrg(month, orgId);
        return ApiResponse.success(snapshots);
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
        return ApiResponse.success(Map.of(
                "month", month,
                "orgId", orgId,
                "count", count
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<PhoneSnapshot> getSnapshot(@PathVariable Long id) {
        PhoneSnapshot snapshot = snapshotService.getSnapshot(id);
        return ApiResponse.success(snapshot);
    }

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
}