package com.phonebiz.controller;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateOrgRequest;
import com.phonebiz.dto.UpdateOrgRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.service.OrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/tree")
    public ApiResponse<List<OrgStructure>> getOrgTree() {
        return ApiResponse.success(orgService.getOrgTree());
    }

    @GetMapping
    public ApiResponse<List<OrgStructure>> getAllOrgs() {
        return ApiResponse.success(orgService.getAllActiveOrgs());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrgStructure> getOrgById(@PathVariable Long id) {
        return ApiResponse.success(orgService.getOrgById(id));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<OrgStructure>> getChildren(@PathVariable Long id) {
        return ApiResponse.success(orgService.getChildren(id));
    }

    @PostMapping
    public ApiResponse<OrgStructure> createOrg(
            @Valid @RequestBody CreateOrgRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(orgService.createOrg(request, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrgStructure> updateOrg(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrgRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(orgService.updateOrg(id, request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrg(@PathVariable Long id) {
        orgService.deleteOrg(id);
        return ApiResponse.success("Organization deleted successfully", null);
    }
}
