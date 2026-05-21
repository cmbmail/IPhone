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
import com.phonebiz.service.WorkOrderDrivenPhoneService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/phone/work-orders")
@PreAuthorize("hasAuthority('wo:create') or hasRole('ADMIN') or hasRole('OPS')")
@RequiredArgsConstructor
public class WorkOrderDrivenPhoneController {

    private final WorkOrderDrivenPhoneService workOrderDrivenPhoneService;
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

    @PostMapping("/allocate")
    public ApiResponse<WorkOrderDTO> allocatePhoneByWorkOrder(@RequestParam Long phoneId,
                                                              @RequestParam Long targetOrgId,
                                                              Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.allocatePhoneByWorkOrder(phoneId, targetOrgId, requesterId, authentication.getName()));
    }

    @PostMapping("/surrender")
    public ApiResponse<WorkOrderDTO> surrenderPhoneByWorkOrder(@RequestParam Long phoneId,
                                                               Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.surrenderPhoneByWorkOrder(phoneId, requesterId, authentication.getName()));
    }

    @PostMapping("/transfer")
    public ApiResponse<WorkOrderDTO> transferPhoneByWorkOrder(@RequestParam Long phoneId,
                                                             @RequestParam Long fromOrgId,
                                                             @RequestParam Long toOrgId,
                                                             Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.transferPhoneByWorkOrder(phoneId, fromOrgId, toOrgId, requesterId, authentication.getName()));
    }

    @PostMapping("/change-number")
    public ApiResponse<WorkOrderDTO> changePhoneNumberByWorkOrder(@RequestParam Long phoneId,
                                                                  @RequestParam String newPhoneNumber,
                                                                  Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.changePhoneNumberByWorkOrder(phoneId, newPhoneNumber, requesterId, authentication.getName()));
    }

    @PostMapping("/change-org")
    public ApiResponse<WorkOrderDTO> changePhoneOrgByWorkOrder(@RequestParam Long phoneId,
                                                               @RequestParam Long newOrgId,
                                                               Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.changePhoneOrgByWorkOrder(phoneId, newOrgId, requesterId, authentication.getName()));
    }

    @PostMapping("/reclaim")
    public ApiResponse<WorkOrderDTO> reclaimPhoneByWorkOrder(@RequestParam Long phoneId,
                                                             Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.reclaimPhoneByWorkOrder(phoneId, requesterId, authentication.getName()));
    }

    @PostMapping("/enable")
    public ApiResponse<WorkOrderDTO> enablePhoneByWorkOrder(@RequestParam Long phoneId,
                                                            Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.enablePhoneByWorkOrder(phoneId, requesterId, authentication.getName()));
    }

    @PostMapping("/disable")
    public ApiResponse<WorkOrderDTO> disablePhoneByWorkOrder(@RequestParam Long phoneId,
                                                              Authentication authentication) {
        Long requesterId = resolveUserId(authentication);
        return ApiResponse.success(workOrderDrivenPhoneService.disablePhoneByWorkOrder(phoneId, requesterId, authentication.getName()));
    }
    private Long resolveUserId(Authentication authentication) {
        return sysUserRepository.findByUsername(authentication.getName())
                .map(SysUser::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004));
    }
}