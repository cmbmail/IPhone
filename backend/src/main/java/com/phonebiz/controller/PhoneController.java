package com.phonebiz.controller;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.service.PhoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Page<PhoneNumber>> getPhones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.success(phoneService.getPhones(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<PhoneNumber> getPhoneById(@PathVariable Long id) {
        return ApiResponse.success(phoneService.getPhoneById(id));
    }

    @GetMapping("/number/{phoneNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<PhoneNumber> getPhoneByNumber(@PathVariable String phoneNumber) {
        return ApiResponse.success(phoneService.getPhoneByNumber(phoneNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'BOSS')")
    public ApiResponse<List<PhoneNumber>> getPhonesByUser(@PathVariable String userId) {
        return ApiResponse.success(phoneService.getPhonesByUser(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Page<PhoneNumber>> getPhonesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PhoneNumber.PhoneStatus phoneStatus = PhoneNumber.PhoneStatus.valueOf(status.toLowerCase());
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(phoneService.getPhonesByStatus(phoneStatus, pageable));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'FINANCE', 'BOSS')")
    public ApiResponse<Page<PhoneHistory>> getPhoneHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(phoneService.getPhoneHistory(id, pageable));
    }

    @GetMapping("/idle")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<List<PhoneNumber>> getIdlePhones() {
        return ApiResponse.success(phoneService.getIdlePhones());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneNumber> createPhone(
            @Valid @RequestBody CreatePhoneRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.createPhone(request, operator));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneNumber> updatePhone(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePhoneRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(phoneService.updatePhone(id, request, operator));
    }
}
