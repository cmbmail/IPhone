package com.phonebiz.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.service.FeatureFlagService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/feature-flags")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/{featureKey}")
    public ApiResponse<SysFeatureFlag> getFeatureFlag(@PathVariable String featureKey) {
        return ApiResponse.success(featureFlagService.getFeatureFlag(featureKey));
    }

    @GetMapping("/{featureKey}/check")
    public ApiResponse<Boolean> checkFeatureEnabled(@PathVariable String featureKey,
                                                   @RequestParam(required = false) Long orgId,
                                                   @RequestParam(required = false) Long userId) {
        boolean enabled = featureFlagService.isFeatureEnabled(featureKey, orgId, userId);
        return ApiResponse.success(enabled);
    }

    @PostMapping
    @AuditLog(module = "system", operation = "创建功能开关", targetType = "SysFeatureFlag")
    public ApiResponse<SysFeatureFlag> createFeatureFlag(@Valid @RequestBody CreateFeatureFlagRequest request) {
        SysFeatureFlag flag = featureFlagService.createFeatureFlag(
                request.getFeatureKey(),
                request.getFeatureName(),
                request.getDescription(),
                request.getIsEnabled(),
                request.getScopeType(),
                request.getScopeValue()
        );
        return ApiResponse.success(flag);
    }

    @PutMapping("/{featureKey}/toggle")
    @AuditLog(module = "system", operation = "切换功能开关", targetType = "SysFeatureFlag", targetId = "#featureKey")
    public ApiResponse<SysFeatureFlag> toggleFeatureFlag(@PathVariable String featureKey,
                                                         @RequestParam boolean isEnabled) {
        return ApiResponse.success(featureFlagService.updateFeatureFlag(featureKey, isEnabled));
    }

    @PutMapping("/{featureKey}/scope")
    @AuditLog(module = "system", operation = "更新开关范围", targetType = "SysFeatureFlag", targetId = "#featureKey")
    public ApiResponse<SysFeatureFlag> updateFeatureFlagScope(@PathVariable String featureKey,
                                                              @RequestParam String scopeType,
                                                              @RequestParam String scopeValue) {
        return ApiResponse.success(featureFlagService.updateFeatureFlagScope(featureKey, scopeType, scopeValue));
    }

    @lombok.Data
    public static class CreateFeatureFlagRequest {
        @NotBlank(message = "featureKey is required")
        private String featureKey;
        @NotBlank(message = "featureName is required")
        private String featureName;
        private String description;
        private Boolean isEnabled;
        private String scopeType;
        private String scopeValue;
    }
}

