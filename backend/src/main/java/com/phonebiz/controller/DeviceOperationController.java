package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.DeviceOperation;
import com.phonebiz.service.DeviceOperationService;
import org.springframework.security.core.Authentication;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/device-operations")
@PreAuthorize("hasAuthority('device:view') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class DeviceOperationController {

    private final DeviceOperationService operationService;

    @GetMapping
    public ApiResponse<Page<DeviceOperation>> getOperations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(operationService.getOperations(pageable));
    }

    @GetMapping("/{id:[0-9]+}")
    public ApiResponse<DeviceOperation> getOperationById(@PathVariable Long id) {
        return ApiResponse.success(operationService.getOperationById(id));
    }

    @GetMapping("/device/{deviceId}")
    public ApiResponse<List<DeviceOperation>> getOperationsByDeviceId(@PathVariable String deviceId) {
        return ApiResponse.success(operationService.getOperationsByDeviceId(deviceId));
    }

    @PostMapping("/{operationId}/execute")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "执行设备操作", targetType = "DeviceOperation", targetId = "#operationId")
    public ApiResponse<DeviceOperation> executeOperation(@PathVariable Long operationId) {
        return ApiResponse.success(operationService.executeOperation(operationId));
    }

    @PostMapping("/{deviceId}/reboot")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "设备重启", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<DeviceOperation> rebootDevice(@PathVariable String deviceId, Authentication authentication) {
        return ApiResponse.success(operationService.rebootDevice(deviceId, authentication != null ? authentication.getName() : "system"));
    }

    @PostMapping("/{deviceId}/sync-config")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "同步设备配置", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<DeviceOperation> syncConfig(@PathVariable String deviceId, Authentication authentication) {
        return ApiResponse.success(operationService.syncConfig(deviceId, authentication != null ? authentication.getName() : "system"));
    }

    @PostMapping("/{deviceId}/upgrade-firmware")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "升级固件", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<DeviceOperation> upgradeFirmware(@PathVariable String deviceId,
                                                       @RequestParam String firmwareVersion,
                                                       Authentication authentication) {
        return ApiResponse.success(operationService.upgradeFirmware(deviceId, firmwareVersion, authentication != null ? authentication.getName() : "system"));
    }

    @PostMapping("/{deviceId}/factory-reset")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "恢复出厂", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<DeviceOperation> factoryReset(@PathVariable String deviceId, Authentication authentication) {
        return ApiResponse.success(operationService.factoryReset(deviceId, authentication != null ? authentication.getName() : "system"));
    }

    @PostMapping("/{deviceId}/register")
    @PreAuthorize("hasAuthority('device:assign') or hasRole('ADMIN')")
    @AuditLog(module = "device", operation = "注册设备", targetType = "Device", targetId = "#deviceId")
    public ApiResponse<DeviceOperation> registerDevice(@PathVariable String deviceId, Authentication authentication) {
        return ApiResponse.success(operationService.registerDevice(deviceId, authentication != null ? authentication.getName() : "system"));
    }
}

