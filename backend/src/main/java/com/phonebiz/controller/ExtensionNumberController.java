package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.ExtensionNumber;
import com.phonebiz.service.ExtensionNumberService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/extension-numbers")
@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
@RequiredArgsConstructor
public class ExtensionNumberController {

    private final ExtensionNumberService extService;

    @GetMapping
    public ApiResponse<Page<ExtensionNumber>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptOrgId,
            @PageableDefault(size = 20, sort = "extensionNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(extService.search(keyword, status, deptOrgId, pageable));
    }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAuthority('phone:assign') or hasRole('ADMIN')")
    @AuditLog(module = "extensionNumber", operation = "分配分机号", targetType = "ExtensionNumber", targetId = "#id")
    public ApiResponse<ExtensionNumber> allocate(@PathVariable Long id,
                                                  @RequestParam String userName,
                                                  @RequestParam(required = false) Long deptOrgId,
                                                  @RequestParam(required = false) String deptName,
                                                  @RequestParam(required = false) String phoneNumber,
                                                  Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResponse.success(extService.allocate(id, userName, deptOrgId, deptName, phoneNumber, operator));
    }

    @PostMapping("/{id}/reclaim")
    @PreAuthorize("hasAuthority('phone:revoke') or hasRole('ADMIN')")
    @AuditLog(module = "extensionNumber", operation = "回收分机号", targetType = "ExtensionNumber", targetId = "#id")
    public ApiResponse<ExtensionNumber> reclaim(@PathVariable Long id, Authentication auth) {
        String operator = auth != null ? auth.getName() : "system";
        return ApiResponse.success(extService.reclaim(id, operator));
    }
}
