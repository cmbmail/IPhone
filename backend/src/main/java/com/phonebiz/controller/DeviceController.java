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
import com.phonebiz.dto.CreateDeviceRequest;
import com.phonebiz.dto.UpdateDeviceRequest;
import com.phonebiz.entity.Device;
import com.phonebiz.service.DeviceService;
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
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

    @GetMapping("/{id:[0-9]+}")
    public ApiResponse<Device> getDeviceById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDeviceById(id));
    }

    @GetMapping("/deviceId/{deviceId}")
    public ApiResponse<Device> getDeviceByDeviceId(@PathVariable String deviceId) {
        return ApiResponse.success(deviceService.getDeviceByDeviceId(deviceId));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<Device>> getDevicesByStatus(@PathVariable String status) {
        Integer deviceStatus = Integer.parseInt(status);
        return ApiResponse.success(deviceService.getDevicesByStatus(deviceStatus));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "创建设备", targetType = "Device")
    public ApiResponse<Device> createDevice(@Valid @RequestBody CreateDeviceRequest request) {
        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .model(request.getModel())
                .macAddress(request.getMacAddress())
                .ipAddress(request.getIpAddress())
                .phoneNumber(request.getPhoneNumber())
                .extensionNumber(request.getExtensionNumber())
                .status(request.getStatus())
                .firmwareVersion(request.getFirmwareVersion())
                .remark(request.getRemark())
                .build();
        return ApiResponse.success(deviceService.createDevice(device));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "更新设备", targetType = "Device", targetId = "#id")
    public ApiResponse<Device> updateDevice(@PathVariable Long id, @Valid @RequestBody UpdateDeviceRequest request) {
        Device existing = deviceService.getDeviceById(id);
        if (request.getDeviceName() != null) existing.setDeviceName(request.getDeviceName());
        if (request.getModel() != null) existing.setModel(request.getModel());
        if (request.getMacAddress() != null) existing.setMacAddress(request.getMacAddress());
        if (request.getIpAddress() != null) existing.setIpAddress(request.getIpAddress());
        if (request.getPhoneNumber() != null) existing.setPhoneNumber(request.getPhoneNumber());
        if (request.getExtensionNumber() != null) existing.setExtensionNumber(request.getExtensionNumber());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getFirmwareVersion() != null) existing.setFirmwareVersion(request.getFirmwareVersion());
        if (request.getRemark() != null) existing.setRemark(request.getRemark());
        return ApiResponse.success(deviceService.updateDevice(id, existing));
    }

    @DeleteMapping("/{id:[0-9]+}")
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
        Integer deviceStatus = Integer.parseInt(status);
        return ApiResponse.success(deviceService.updateStatus(deviceId, deviceStatus));
    }

    @PostMapping("/{deviceId}/checkin")
    @PreAuthorize("hasAuthority('device:view') or hasRole('ADMIN') or hasRole('OPS')")
    @AuditLog(module = "device", operation = "设备签到", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<Device> checkin(@PathVariable String deviceId,
                                      @RequestParam String ipAddress,
                                      @RequestParam(required = false) String firmwareVersion) {
        // C-03: Validate IP address format to prevent SSRF
        if (!ipAddress.matches("^[0-9a-fA-F.:]+$")) {
            throw new BusinessException(ErrorCode.SYS_002, "Invalid IP address format");
        }
        return ApiResponse.success(deviceService.checkin(deviceId, ipAddress, firmwareVersion));
    }
}

