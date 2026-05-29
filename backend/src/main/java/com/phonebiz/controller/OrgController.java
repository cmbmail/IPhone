package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateOrgRequest;
import com.phonebiz.dto.ImportOrgItem;
import com.phonebiz.dto.UpdateOrgRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.service.OrgService;
import com.phonebiz.security.DataScope;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/orgs")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;
    private final DataScope dataScope;

    @GetMapping("/tree")
    public ApiResponse<List<OrgStructure>> getOrgTree() {
        return ApiResponse.success(orgService.getOrgTree());
    }

    @GetMapping
    public ApiResponse<List<OrgStructure>> getAllOrgs() {
        return ApiResponse.success(orgService.getAllActiveOrgs());
    }

    @GetMapping("/{id:[0-9]+}")
    public ApiResponse<OrgStructure> getOrgById(@PathVariable Long id) {
        return ApiResponse.success(orgService.getOrgById(id));
    }

    @GetMapping("/{id:[0-9]+}/children")
    public ApiResponse<List<OrgStructure>> getChildren(@PathVariable Long id) {
        return ApiResponse.success(orgService.getChildren(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:create') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "创建组织", targetType = "OrgStructure", targetId = "#request.parentId")
    public ApiResponse<OrgStructure> createOrg(
            @Valid @RequestBody CreateOrgRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(orgService.createOrg(request, operator));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('org:edit') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "更新组织", targetType = "OrgStructure", targetId = "#id")
    public ApiResponse<OrgStructure> updateOrg(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrgRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(orgService.updateOrg(id, request, operator));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('org:delete') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "删除组织", targetType = "OrgStructure", targetId = "#id")
    public ApiResponse<Void> deleteOrg(@PathVariable Long id) {
        orgService.deleteOrg(id);
        return ApiResponse.success("Organization deleted successfully", null);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('org:create') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "批量导入组织", targetType = "OrgStructure")
    public ApiResponse<Map<String, Object>> importOrgs(
            @RequestBody List<ImportOrgItem> items,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        int created = orgService.importOrgs(items, operator);
        return ApiResponse.success(Map.of("created", created));
    }

    @PostMapping("/import-cost-center")
    @PreAuthorize("hasAuthority('org:import') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "导入成本中心", targetType = "OrgStructure")
    public ApiResponse<Map<String, Object>> importCostCenter(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        Map<String, Object> result = orgService.importCostCenter(file, operator);
        return ApiResponse.success(result);
    }

    @GetMapping("/tree/scope")
    public ApiResponse<List<OrgStructure>> getOrgTreeByScope(Authentication authentication) {
        return ApiResponse.success(orgService.getOrgTreeByScope(dataScope.getCurrentScopeOrgId()));
    }

    @PutMapping("/sort")
    @PreAuthorize("hasAuthority('org:edit') or hasRole('ADMIN')")
    @AuditLog(module = "org", operation = "调整排序", targetType = "OrgStructure")
    public ApiResponse<Void> updateSortOrders(
            @RequestBody Map<Long, Integer> sortOrderMap) {
        // M-10: Validate map size to prevent DoS
        if (sortOrderMap == null || sortOrderMap.isEmpty()) {
            throw new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.SYS_002, "Sort order map cannot be empty");
        }
        if (sortOrderMap.size() > 500) {
            throw new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.SYS_002, "Sort order map too large, max 500 entries");
        }
        orgService.updateSortOrders(sortOrderMap);
        return ApiResponse.success("Sort order updated", null);
    }
}
