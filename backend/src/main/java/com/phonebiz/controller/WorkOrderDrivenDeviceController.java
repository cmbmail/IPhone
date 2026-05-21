package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.service.FeatureFlagService;
import com.phonebiz.service.WorkOrderDrivenDeviceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/phone-device/work-orders")
@PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class WorkOrderDrivenDeviceController {

    private final WorkOrderDrivenDeviceService workOrderDrivenDeviceService;
    private final FeatureFlagService featureFlagService;
    private final SysUserRepository sysUserRepository;

    @GetMapping("/feature/enabled")
    public ApiResponse<Boolean> isWorkOrderDrivenEnabled() {
        boolean enabled = featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN);
        return ApiResponse.success(enabled);
    }

    @GetMapping("/feature/{featureKey}")
    public ApiResponse<SysFeatureFlag> getFeatureFlag(@PathVariable String featureKey) {
        return ApiResponse.success(featureFlagService.getFeatureFlag(featureKey));
    }

    @GetMapping("/feature/{featureKey}/check")
    public ApiResponse<Boolean> checkFeatureEnabled(@PathVariable String featureKey,
                                                   @RequestParam(required = false) Long orgId,
                                                   @RequestParam(required = false) Long userId) {
        boolean enabled = featureFlagService.isFeatureEnabled(featureKey, orgId, userId);
        return ApiResponse.success(enabled);
    }

    @PostMapping("/assign")
    public ApiResponse<WorkOrderDTO> assignDeviceByWorkOrder(@RequestParam Long deviceId,
                                                            @RequestParam String employeeNo,
                                                            Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.assignDeviceByWorkOrder(deviceId, employeeNo, requesterId, authentication.getName()));
    }

    @PostMapping("/reclaim")
    public ApiResponse<WorkOrderDTO> reclaimDeviceByWorkOrder(@RequestParam Long deviceId,
                                                            Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.reclaimDeviceByWorkOrder(deviceId, requesterId, authentication.getName()));
    }

    @PostMapping("/repair")
    public ApiResponse<WorkOrderDTO> repairDeviceByWorkOrder(@RequestParam Long deviceId,
                                                           Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.repairDeviceByWorkOrder(deviceId, requesterId, authentication.getName()));
    }

    @PostMapping("/retire")
    public ApiResponse<WorkOrderDTO> retireDeviceByWorkOrder(@RequestParam Long deviceId,
                                                          Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.retireDeviceByWorkOrder(deviceId, requesterId, authentication.getName()));
    }

    @PostMapping("/bind-phone")
    public ApiResponse<WorkOrderDTO> bindPhoneToDeviceByWorkOrder(@RequestParam Long deviceId,
                                                                 @RequestParam String extensionNumber,
                                                                 Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.bindPhoneToDeviceByWorkOrder(deviceId, extensionNumber, requesterId, authentication.getName()));
    }

    @PostMapping("/unbind-phone")
    public ApiResponse<WorkOrderDTO> unbindPhoneFromDeviceByWorkOrder(@RequestParam Long deviceId,
                                                                     @RequestParam String extensionNumber,
                                                                     Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenDeviceService.unbindPhoneFromDeviceByWorkOrder(deviceId, extensionNumber, requesterId, authentication.getName()));
    }
    private Long resolveUserId(Authentication authentication) {
        return sysUserRepository.findByUsername(authentication.getName())
                .map(SysUser::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004));
    }
}