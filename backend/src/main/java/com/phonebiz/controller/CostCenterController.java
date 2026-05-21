package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateCostCenterRequest;
import com.phonebiz.dto.UpdateCostCenterRequest;
import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.service.CostCenterService;

@RestController
@RequestMapping("/cost-centers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
public class CostCenterController {

    private final CostCenterService costCenterService;

    @GetMapping
    public ApiResponse<List<CostCenterMapping>> getAllCostCenters() {
        return ApiResponse.success(costCenterService.getAllCostCenters());
    }

    @GetMapping("/{id}")
    public ApiResponse<CostCenterMapping> getCostCenterById(@PathVariable Long id) {
        return ApiResponse.success(costCenterService.getCostCenterById(id));
    }

    @GetMapping("/org/{orgId}")
    public ApiResponse<List<CostCenterMapping>> getCostCentersByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(costCenterService.getCostCentersByOrg(orgId));
    }

    @PostMapping
    public ApiResponse<CostCenterMapping> createCostCenter(
            @Valid @RequestBody CreateCostCenterRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(costCenterService.createCostCenter(request, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<CostCenterMapping> updateCostCenter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCostCenterRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(costCenterService.updateCostCenter(id, request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCostCenter(@PathVariable Long id) {
        costCenterService.deleteCostCenter(id);
        return ApiResponse.success("Cost center deleted successfully", null);
    }
}

