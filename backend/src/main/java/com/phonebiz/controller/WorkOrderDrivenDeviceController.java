package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.service.FeatureFlagService;
import com.phonebiz.service.WorkOrderDrivenDeviceService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/phone-device/work-orders")
@PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class WorkOrderDrivenDeviceController {

    private final WorkOrderDrivenDeviceService workOrderDrivenDeviceService;
    private final FeatureFlagService featureFlagService;

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
                                                            @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                            @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.assignDeviceByWorkOrder(deviceId, employeeNo, requesterId, requesterName));
    }

    @PostMapping("/reclaim")
    public ApiResponse<WorkOrderDTO> reclaimDeviceByWorkOrder(@RequestParam Long deviceId,
                                                            @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                            @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.reclaimDeviceByWorkOrder(deviceId, requesterId, requesterName));
    }

    @PostMapping("/repair")
    public ApiResponse<WorkOrderDTO> repairDeviceByWorkOrder(@RequestParam Long deviceId,
                                                           @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                           @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.repairDeviceByWorkOrder(deviceId, requesterId, requesterName));
    }

    @PostMapping("/retire")
    public ApiResponse<WorkOrderDTO> retireDeviceByWorkOrder(@RequestParam Long deviceId,
                                                          @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                          @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.retireDeviceByWorkOrder(deviceId, requesterId, requesterName));
    }

    @PostMapping("/bind-phone")
    public ApiResponse<WorkOrderDTO> bindPhoneToDeviceByWorkOrder(@RequestParam Long deviceId,
                                                                 @RequestParam String extensionNumber,
                                                                 @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                                 @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.bindPhoneToDeviceByWorkOrder(deviceId, extensionNumber, requesterId, requesterName));
    }

    @PostMapping("/unbind-phone")
    public ApiResponse<WorkOrderDTO> unbindPhoneFromDeviceByWorkOrder(@RequestParam Long deviceId,
                                                                     @RequestParam String extensionNumber,
                                                                     @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                                     @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenDeviceService.unbindPhoneFromDeviceByWorkOrder(deviceId, extensionNumber, requesterId, requesterName));
    }
}

