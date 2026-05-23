package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneChangeRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.PhoneReleaseRequest;
import com.phonebiz.dto.PhoneReserveRequest;
import com.phonebiz.dto.PhoneStatusChangeRequest;
import com.phonebiz.dto.PhoneSurrenderRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSurrenderRecord;
import com.phonebiz.service.PhoneService;
import com.phonebiz.dto.PhoneViewDTO;

@RestController
@RequestMapping("/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;
    private final com.phonebiz.service.PhoneViewService phoneViewService;

    @GetMapping
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<Page<PhoneNumber>> getPhones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(phoneService.getPhones(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> getPhoneById(@PathVariable Long id) {
        return ApiResponse.success(phoneService.getPhoneById(id));
    }

    @GetMapping("/number/{phoneNumber}")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> getPhoneByNumber(@PathVariable String phoneNumber) {
        return ApiResponse.success(phoneService.getPhoneByNumber(phoneNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<List<PhoneNumber>> getPhonesByUser(@PathVariable String userId) {
        return ApiResponse.success(phoneService.getPhonesByUser(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<Page<PhoneNumber>> getPhonesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Integer phoneStatus = Integer.parseInt(status);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(phoneService.getPhonesByStatus(phoneStatus, pageable));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<Page<PhoneHistory>> getPhoneHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(phoneService.getPhoneHistory(id, pageable));
    }

    @GetMapping("/idle")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<List<PhoneNumber>> getIdlePhones() {
        return ApiResponse.success(phoneService.getIdlePhones());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('phone:import') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "创建号码", targetType = "PhoneNumber")
    public ApiResponse<PhoneNumber> createPhone(
            @Valid @RequestBody CreatePhoneRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.createPhone(request, operator));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "更新号码", targetType = "PhoneNumber", targetId = "#id")
    public ApiResponse<PhoneNumber> updatePhone(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePhoneRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.updatePhone(id, request, operator));
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAuthority('phone:assign') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "分配号码", targetType = "PhoneNumber", targetId = "#phoneId")
    public ApiResponse<PhoneNumber> allocatePhone(
            @Valid @RequestBody PhoneAllocationRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.allocatePhone(request, operator));
    }

    @PostMapping("/reclaim")
    @PreAuthorize("hasAuthority('phone:revoke') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "回收号码", targetType = "PhoneNumber", targetId = "#phoneId")
    public ApiResponse<PhoneNumber> reclaimPhone(
            @Valid @RequestBody PhoneReclaimRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.reclaimPhone(request, operator));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> changeStatus(
            @Valid @RequestBody PhoneStatusChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        Integer newStatus = request.getNewStatus();
        return ApiResponse.success(phoneService.changeStatus(request.getPhoneId(), newStatus, operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/surrender")
    @PreAuthorize("hasAuthority('phone:surrender') or hasAuthority('phone:view') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "交回号码", targetType = "PhoneNumber", targetId = "#phoneId")
    public ApiResponse<PhoneSurrenderRecord> surrenderPhone(
            @Valid @RequestBody PhoneSurrenderRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.surrenderPhone(request.getPhoneId(), request.getSurrenderType(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAuthority('phone:reserve') or hasAuthority('phone:view') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "预留号码", targetType = "PhoneNumber", targetId = "#phoneId")
    public ApiResponse<PhoneNumber> reservePhone(
            @Valid @RequestBody PhoneReserveRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.reservePhone(request.getPhoneId(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAuthority('phone:revoke') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "释放号码", targetType = "PhoneNumber", targetId = "#phoneId")
    public ApiResponse<PhoneNumber> releasePhone(
            @Valid @RequestBody PhoneReleaseRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.releasePhone(request.getPhoneId(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/change-user")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> changeUser(
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.changeUser(request.getPhoneId(), request.getEmployeeNo(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/change-org")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> changeOrg(
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.changeOrg(request.getPhoneId(), request.getOrgId(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/change-number")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> changeNumber(
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.changeNumber(request.getPhoneId(), request.getPhoneNumber(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/change-extension")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<PhoneNumber> changeExtension(
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.changeExtension(request.getPhoneId(), request.getExtensionNumber(), operator, request.getWorkOrderNo(), request.getRemark()));
    }

    @PostMapping("/change")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    @AuditLog(module = "phone", operation = "批量变更号码", targetType = "PhoneNumber")
    public ApiResponse<PhoneNumber> batchChange(
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.batchChange(request.getPhoneId(), request, operator));
    }

    @PostMapping("/batch-change")
    @PreAuthorize("hasAuthority('phone:change') or hasRole('ADMIN')")
    public ApiResponse<Integer> batchChangeMultiple(
            @RequestParam List<Long> phoneIds,
            @Valid @RequestBody PhoneChangeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        int successCount = phoneService.batchChangeMultiple(phoneIds, request, operator);
        return ApiResponse.success(successCount);
    }
}

