package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.repository.SysFeatureFlagRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final SysFeatureFlagRepository featureFlagRepository;

    public static final String FEATURE_WORK_ORDER_DRIVEN = "phone.operation.work_order_driven";
    public static final String FEATURE_PHONE_ALLOCATE = "phone.allocate.work_order_required";
    public static final String FEATURE_PHONE_SURRENDER = "phone.surrender.work_order_required";
    public static final String FEATURE_PHONE_TRANSFER = "phone.transfer.work_order_required";
    public static final String FEATURE_PHONE_CHANGE_NUMBER = "phone.change_number.work_order_required";
    public static final String FEATURE_PHONE_CHANGE_ORG = "phone.change_org.work_order_required";
    public static final String FEATURE_PHONE_RECLAIM = "phone.reclaim.work_order_required";
    public static final String FEATURE_PHONE_ENABLE = "phone.enable.work_order_required";
    public static final String FEATURE_PHONE_DISABLE = "phone.disable.work_order_required";

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureKey) {
        return isFeatureEnabled(featureKey, null, null);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureKey, Long orgId, Long userId) {
        Optional<SysFeatureFlag> feature = featureFlagRepository.findByFeatureKey(featureKey);
        
        if (feature.isEmpty()) {
            return false;
        }

        SysFeatureFlag flag = feature.get();
        
        if (!flag.getIsEnabled()) {
            return false;
        }

        if (flag.getStartTime() != null && LocalDateTime.now().isBefore(flag.getStartTime())) {
            return false;
        }

        if (flag.getEndTime() != null && LocalDateTime.now().isAfter(flag.getEndTime())) {
            return false;
        }

        if (flag.getScopeType() == null || "ALL".equals(flag.getScopeType())) {
            return true;
        }

        if ("ORGANIZATION".equals(flag.getScopeType()) && orgId != null) {
            return flag.getScopeValue() != null && 
                   flag.getScopeValue().contains(orgId.toString());
        }

        if ("USER".equals(flag.getScopeType()) && userId != null) {
            return flag.getScopeValue() != null && 
                   flag.getScopeValue().contains(userId.toString());
        }

        return false;
    }

    @Transactional(readOnly = true)
    public SysFeatureFlag getFeatureFlag(String featureKey) {
        return featureFlagRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_003));
    }

    @Transactional
    public SysFeatureFlag createFeatureFlag(String featureKey, String featureName, String description, 
                                           boolean isEnabled, String scopeType, String scopeValue) {
        if (featureFlagRepository.existsByFeatureKey(featureKey)) {
            throw new BusinessException(ErrorCode.SYS_004);
        }

        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey(featureKey)
                .featureName(featureName)
                .description(description)
                .isEnabled(isEnabled)
                .scopeType(scopeType)
                .scopeValue(scopeValue)
                .build();

        flag.setCreatedAt(LocalDateTime.now());
        flag.setUpdatedAt(LocalDateTime.now());

        return featureFlagRepository.save(flag);
    }

    @Transactional
    public SysFeatureFlag updateFeatureFlag(String featureKey, boolean isEnabled) {
        SysFeatureFlag flag = featureFlagRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_003));

        flag.setIsEnabled(isEnabled);
        flag.setUpdatedAt(LocalDateTime.now());

        return featureFlagRepository.save(flag);
    }

    @Transactional
    public SysFeatureFlag updateFeatureFlagScope(String featureKey, String scopeType, String scopeValue) {
        SysFeatureFlag flag = featureFlagRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_003));

        flag.setScopeType(scopeType);
        flag.setScopeValue(scopeValue);
        flag.setUpdatedAt(LocalDateTime.now());

        return featureFlagRepository.save(flag);
    }
}

