package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateAreaCodeMappingRequest;
import com.phonebiz.entity.AreaCodeOrgMapping;
import com.phonebiz.service.AreaCodeService;

@RestController
@RequestMapping("/area-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
public class AreaCodeController {

    private final AreaCodeService areaCodeService;

    @GetMapping
    public ApiResponse<List<AreaCodeOrgMapping>> getAllMappings() {
        return ApiResponse.success(areaCodeService.getAllMappings());
    }

    @GetMapping("/{id}")
    public ApiResponse<AreaCodeOrgMapping> getMappingById(@PathVariable Long id) {
        return ApiResponse.success(areaCodeService.getMappingById(id));
    }

    @GetMapping("/area/{areaCode}")
    public ApiResponse<List<AreaCodeOrgMapping>> getMappingsByAreaCode(@PathVariable String areaCode) {
        return ApiResponse.success(areaCodeService.getMappingsByAreaCode(areaCode));
    }

    @GetMapping("/org/{orgId}")
    public ApiResponse<List<AreaCodeOrgMapping>> getMappingsByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(areaCodeService.getMappingsByOrg(orgId));
    }

    @GetMapping("/match/{areaCode}")
    public ApiResponse<Long> matchOrgByAreaCode(@PathVariable String areaCode) {
        return ApiResponse.success(areaCodeService.matchOrgByAreaCode(areaCode));
    }

    @PostMapping
    public ApiResponse<AreaCodeOrgMapping> createMapping(
            @Valid @RequestBody CreateAreaCodeMappingRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(areaCodeService.createMapping(request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMapping(@PathVariable Long id) {
        areaCodeService.deleteMapping(id);
        return ApiResponse.success("Area code mapping deleted successfully", null);
    }
}

