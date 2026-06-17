package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.FeeAllocationDTO;
import com.phonebiz.dto.FeeAllocationDTO.LevelResponse;
import com.phonebiz.service.FeeAllocationService;

@RestController
@RequestMapping("/fee-allocations")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FeeAllocationController {

    private final FeeAllocationService feeAllocationService;

    /** Calculate and save allocations for a given level */
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('bill:allocate')")
    public ResponseEntity<ApiResponse<LevelResponse>> calculate(
            @RequestParam String billMonth,
            @RequestParam int level) {
        LevelResponse response = feeAllocationService.calculateAndSave(billMonth, level);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Get level 1 allocations (总行→一级分行) */
    @GetMapping("/level1")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevel1(@RequestParam String billMonth) {
        LevelResponse response = feeAllocationService.getAllocationLevel(billMonth, 1);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Get level 2 allocations (一级分行→二级分行/部门) */
    @GetMapping("/level2")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevel2(
            @RequestParam String billMonth,
            @RequestParam(required = false) Long parentOrgId) {
        if (parentOrgId != null) {
            LevelResponse response = feeAllocationService.getAllocationByParent(billMonth, 2, parentOrgId);
            return ResponseEntity.ok(ApiResponse.success(response));
        }
        LevelResponse response = feeAllocationService.getAllocationLevel(billMonth, 2);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Get level 3 allocations (二级分行→部门) */
    @GetMapping("/level3")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevel3(
            @RequestParam String billMonth,
            @RequestParam(required = false) Long parentOrgId) {
        if (parentOrgId != null) {
            LevelResponse response = feeAllocationService.getAllocationByParent(billMonth, 3, parentOrgId);
            return ResponseEntity.ok(ApiResponse.success(response));
        }
        LevelResponse response = feeAllocationService.getAllocationLevel(billMonth, 3);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Get available parent org IDs for dropdown (level 2 or 3) */
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<List<Long>>> getParentOrgIds(
            @RequestParam String billMonth,
            @RequestParam int level) {
        List<Long> ids = feeAllocationService.getParentOrgIds(billMonth, level);
        return ResponseEntity.ok(ApiResponse.success(ids));
    }

    /** Confirm an allocation */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('bill:allocate')")
    public ResponseEntity<ApiResponse<FeeAllocationDTO>> confirm(
            @PathVariable Long id, Authentication auth) {
        FeeAllocationDTO dto = feeAllocationService.confirmAllocation(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /** Reject an allocation */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('bill:allocate')")
    public ResponseEntity<ApiResponse<FeeAllocationDTO>> reject(
            @PathVariable Long id, Authentication auth) {
        FeeAllocationDTO dto = feeAllocationService.rejectAllocation(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
