package com.phonebiz.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.phonebiz.annotation.AuditLog;
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
        Integer statusEnum = status != null ? Integer.valueOf(status) : null;
        Page<PhoneDeviceDTO> result = phoneDeviceService.getDeviceList(orgIds, statusEnum, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> getDeviceDetail(@PathVariable Long id) {
        PhoneDeviceDTO device = phoneDeviceService.getDeviceDetail(id);
        return ApiResponse.success(device);
    }

    @GetMapping("/{id:[0-9]+}/phones")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<List<BoundPhoneDTO>> getBoundPhones(@PathVariable Long id) {
        List<BoundPhoneDTO> phones = phoneDeviceService.getBoundPhones(id);
        return ApiResponse.success(phones);
    }

    @GetMapping("/{id:[0-9]+}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<List<PhoneDeviceHistoryDTO>> getDeviceHistory(@PathVariable Long id) {
        List<PhoneDeviceHistoryDTO> history = phoneDeviceService.getDeviceHistory(id);
        return ApiResponse.success(history);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping
    @PreAuthorize("hasAuthority('device:assign') or hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> createDevice(@Valid @RequestBody CreatePhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.createDevice(request);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('device:assign') or hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> updateDevice(@PathVariable Long id, @Valid @RequestBody UpdatePhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.updateDevice(id, request);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/assign")
    @PreAuthorize("hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> assignDevice(@PathVariable Long id, @Valid @RequestBody AssignPhoneDeviceRequest request) {
        PhoneDeviceDTO device = phoneDeviceService.assignDevice(id, request);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/reclaim")
    @PreAuthorize("hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> reclaimDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.reclaimDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/deactivate")
    @PreAuthorize("hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> deactivateDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.deactivateDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/reactivate")
    @PreAuthorize("hasAuthority('device:manage') or hasRole('ADMIN')")
    public ApiResponse<PhoneDeviceDTO> reactivateDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.reactivateDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/repair")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> repairDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.repairDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/repair-done")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> repairDoneDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.repairDoneDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/retire")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<PhoneDeviceDTO> retireDevice(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        PhoneDeviceDTO device = phoneDeviceService.retireDevice(id, remark);
        return ApiResponse.success(device);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @PostMapping("/{id:[0-9]+}/bind-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> bindPhone(@PathVariable Long id, @Valid @RequestBody BindPhoneRequest request) {
        phoneDeviceService.bindPhone(id, request);
        return ApiResponse.success(null);
    }

    @AuditLog(module = "phonedevice", operation = "PhoneDevice 操作")
    @DeleteMapping("/{id:[0-9]+}/unbind-phone/{phoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ApiResponse<Void> unbindPhone(@PathVariable Long id, @PathVariable Long phoneId) {
        phoneDeviceService.unbindPhone(id, phoneId);
        return ApiResponse.success(null);
    }
}

