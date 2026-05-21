package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.*;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.service.PhoneDeviceService;

@RestController
@RequestMapping("/phone-devices")
@RequiredArgsConstructor
public class PhoneDeviceController {
    private final PhoneDeviceService phoneDeviceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Page<PhoneDeviceDTO>> getDeviceList(
            @RequestParam(required = false) List<Long> orgIds,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PhoneDevice.PhoneDeviceStatus statusEnum = status != null ? PhoneDevice.PhoneDeviceStatus.valueOf(status) : null;
        Page<PhoneDeviceDTO> result = phoneDeviceService.getDeviceList(orgIds, statusEnum, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> getDeviceDetail(@PathVariable Long id) {
        PhoneDeviceDTO device = phoneDeviceService.getDeviceDetail(id);
        return ApiResponse.success(device);
    }

    @GetMapping("/{id}/phones")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<List<BoundPhoneDTO>> getBoundPhones(@PathVariable Long id) {
        List<BoundPhoneDTO> phones = phoneDeviceService.getBoundPhones(id);
        return ApiResponse.success(phones);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<List<PhoneDeviceHistoryDTO>> getDeviceHistory(@PathVariable Long id) {
        List<PhoneDeviceHistoryDTO> history = phoneDeviceService.getDeviceHistory(id);
        return ApiResponse.success(history);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPS')")
    public ApiResponse<PhoneDeviceDTO> createDevice(@Valid @RequestBody CreatePhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.createDevice(request);
        return ApiResponse.success(device);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPS')")
    public ApiResponse<PhoneDeviceDTO> updateDevice(@PathVariable Long id, @Valid @RequestBody UpdatePhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.updateDevice(id, request);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> assignDevice(@PathVariable Long id, @Valid @RequestBody AssignPhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.assignDevice(id, request);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/reclaim")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> reclaimDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.reclaimDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> deactivateDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.deactivateDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> reactivateDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.reactivateDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/repair")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> repairDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.repairDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/repair-done")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> repairDoneDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.repairDoneDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> retireDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.retireDevice(id, remark);
        return ApiResponse.success(device);
    }

    @PostMapping("/{id}/bind-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> bindPhone(@PathVariable Long id, @Valid @RequestBody BindPhoneRequest request) {
        phoneDeviceService.bindPhone(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/unbind-phone/{phoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> unbindPhone(@PathVariable Long id, @PathVariable Long phoneId) {
        phoneDeviceService.unbindPhone(id, phoneId);
        return ApiResponse.success(null);
    }
}

