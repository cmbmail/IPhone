package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateExtensionPoolRequest;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.service.ExtensionPoolService;

@RestController
@RequestMapping("/extension-pools")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
public class ExtensionPoolController {

    private final ExtensionPoolService poolService;

    @GetMapping
    public ApiResponse<List<ExtensionPool>> getAllPools() {
        return ApiResponse.success(poolService.getAllPools());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExtensionPool> getPoolById(@PathVariable Long id) {
        return ApiResponse.success(poolService.getPoolById(id));
    }

    @GetMapping("/org/{orgId}")
    public ApiResponse<List<ExtensionPool>> getPoolsByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(poolService.getPoolsByOrg(orgId));
    }

    @GetMapping("/{id}/usage")
    public ApiResponse<ExtensionPoolService.ExtensionPoolUsage> getPoolUsage(@PathVariable Long id) {
        return ApiResponse.success(poolService.getPoolUsage(id));
    }

    @AuditLog(module = "extensionpool", operation = "ExtensionPool 操作")
    @PostMapping
    public ApiResponse<ExtensionPool> createPool(
            @Valid @RequestBody CreateExtensionPoolRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(poolService.createPool(request, operator));
    }

    @AuditLog(module = "extensionpool", operation = "ExtensionPool 操作")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePool(@PathVariable Long id) {
        poolService.deletePool(id);
        return ApiResponse.success("Extension pool deleted successfully", null);
    }

    @GetMapping("/{id}/exhaustion")
    public ApiResponse<ExtensionPoolService.PoolExhaustionInfo> checkExhaustion(@PathVariable Long id) {
        return ApiResponse.success(poolService.checkExhaustion(id));
    }

    @GetMapping("/org/{orgId}/suggestions")
    public ApiResponse<List<String>> getSuggestions(@PathVariable Long orgId) {
        return ApiResponse.success(poolService.suggestAlternatePools(orgId));
    }

    @GetMapping("/overlap-check")
    public ApiResponse<Boolean> checkOverlap(
            @RequestParam Long orgId,
            @RequestParam String startNumber,
            @RequestParam String endNumber) {
        return ApiResponse.success(poolService.checkOverlap(orgId, startNumber, endNumber));
    }

    @GetMapping("/all-usage")
    public ApiResponse<List<ExtensionPoolService.ExtensionPoolUsage>> getAllPoolUsages() {
        return ApiResponse.success(poolService.getAllPoolUsages());
    }

    @GetMapping("/warnings")
    public ApiResponse<List<ExtensionPoolService.ExtensionPoolUsage>> getWarningPools() {
        return ApiResponse.success(poolService.getWarningPools());
    }
}

