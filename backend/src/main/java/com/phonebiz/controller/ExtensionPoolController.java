package com.phonebiz.controller;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateExtensionPoolRequest;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.service.ExtensionPoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public ApiResponse<ExtensionPool> createPool(
            @Valid @RequestBody CreateExtensionPoolRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(poolService.createPool(request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePool(@PathVariable Long id) {
        poolService.deletePool(id);
        return ApiResponse.success("Extension pool deleted successfully", null);
    }
}
