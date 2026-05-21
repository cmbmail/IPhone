package com.phonebiz.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.Device;
import com.phonebiz.service.DeviceService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/devices")
@PreAuthorize("hasAuthority('device:view') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ApiResponse<Page<Device>> getDevices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(deviceService.getDevices(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<Device> getDeviceById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDeviceById(id));
    }

    @GetMapping("/deviceId/{deviceId}")
    public ApiResponse<Device> getDeviceByDeviceId(@PathVariable String deviceId) {
        return ApiResponse.success(deviceService.getDeviceByDeviceId(deviceId));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<Device>> getDevicesByStatus(@PathVariable String status) {
        Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(deviceService.getDevicesByStatus(deviceStatus));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "创建设备", targetType = "Device")
    public ApiResponse<Device> createDevice(@Valid @RequestBody Device device) {
        return ApiResponse.success(deviceService.createDevice(device));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "更新设备", targetType = "Device", targetId = "#id")
    public ApiResponse<Device> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        return ApiResponse.success(deviceService.updateDevice(id, device));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('device:revoke') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "删除设备", targetType = "Device", targetId = "#id")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success("Device deleted", null);
    }

    @PostMapping("/{deviceId}/status")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "变更设备状态", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<Device> updateStatus(@PathVariable String deviceId, 
                                           @RequestParam String status) {
        Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(deviceService.updateStatus(deviceId, deviceStatus));
    }

    @PostMapping("/{deviceId}/checkin")
    @PreAuthorize("hasAuthority('device:view') or hasRole('ADMIN') or hasRole('OPS')")
    @AuditLog(module = "device", operation = "设备签到", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<Device> checkin(@PathVariable String deviceId,
                                      @RequestParam String ipAddress,
                                      @RequestParam(required = false) String firmwareVersion) {
        return ApiResponse.success(deviceService.checkin(deviceId, ipAddress, firmwareVersion));
    }
}

