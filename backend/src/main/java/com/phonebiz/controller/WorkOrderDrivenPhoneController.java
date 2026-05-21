package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.service.FeatureFlagService;
import com.phonebiz.service.WorkOrderDrivenPhoneService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/phone/work-orders")
@PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class WorkOrderDrivenPhoneController {

    private final WorkOrderDrivenPhoneService workOrderDrivenPhoneService;
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

    @PostMapping("/allocate")
    public ApiResponse<WorkOrderDTO> allocatePhoneByWorkOrder(@RequestParam Long phoneId,
                                                              @RequestParam Long targetOrgId,
                                                              @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                              @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.allocatePhoneByWorkOrder(phoneId, targetOrgId, requesterId, requesterName));
    }

    @PostMapping("/surrender")
    public ApiResponse<WorkOrderDTO> surrenderPhoneByWorkOrder(@RequestParam Long phoneId,
                                                               @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                               @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.surrenderPhoneByWorkOrder(phoneId, requesterId, requesterName));
    }

    @PostMapping("/transfer")
    public ApiResponse<WorkOrderDTO> transferPhoneByWorkOrder(@RequestParam Long phoneId,
                                                             @RequestParam Long fromOrgId,
                                                             @RequestParam Long toOrgId,
                                                             @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                             @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.transferPhoneByWorkOrder(phoneId, fromOrgId, toOrgId, requesterId, requesterName));
    }

    @PostMapping("/change-number")
    public ApiResponse<WorkOrderDTO> changePhoneNumberByWorkOrder(@RequestParam Long phoneId,
                                                                  @RequestParam String newPhoneNumber,
                                                                  @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                                  @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.changePhoneNumberByWorkOrder(phoneId, newPhoneNumber, requesterId, requesterName));
    }

    @PostMapping("/change-org")
    public ApiResponse<WorkOrderDTO> changePhoneOrgByWorkOrder(@RequestParam Long phoneId,
                                                               @RequestParam Long newOrgId,
                                                               @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                               @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.changePhoneOrgByWorkOrder(phoneId, newOrgId, requesterId, requesterName));
    }

    @PostMapping("/reclaim")
    public ApiResponse<WorkOrderDTO> reclaimPhoneByWorkOrder(@RequestParam Long phoneId,
                                                             @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                             @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.reclaimPhoneByWorkOrder(phoneId, requesterId, requesterName));
    }

    @PostMapping("/enable")
    public ApiResponse<WorkOrderDTO> enablePhoneByWorkOrder(@RequestParam Long phoneId,
                                                            @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                            @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.enablePhoneByWorkOrder(phoneId, requesterId, requesterName));
    }

    @PostMapping("/disable")
    public ApiResponse<WorkOrderDTO> disablePhoneByWorkOrder(@RequestParam Long phoneId,
                                                              @RequestParam(required = false, defaultValue = "1") Long requesterId,
                                                              @RequestParam(required = false, defaultValue = "admin") String requesterName) {
        return ApiResponse.success(workOrderDrivenPhoneService.disablePhoneByWorkOrder(phoneId, requesterId, requesterName));
    }
}

