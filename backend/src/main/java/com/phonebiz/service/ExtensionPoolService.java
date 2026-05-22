package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateExtensionPoolRequest;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.ExtensionPoolRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtensionPoolService {

    private final ExtensionPoolRepository poolRepository;
    private final OrgStructureRepository orgRepository;
    private final PhoneNumberRepository phoneRepository;

    @Transactional(readOnly = true)
    public List<ExtensionPool> getAllPools() {
        return poolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ExtensionPool getPoolById(Long id) {
        return poolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED));
    }

    @Transactional(readOnly = true)
    public List<ExtensionPool> getPoolsByOrg(Long orgId) {
        return poolRepository.findByOrgId(orgId);
    }

    @Transactional
    public ExtensionPool createPool(CreateExtensionPoolRequest request, String operator) {
        OrgStructure org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        if (!isValidRange(request.getStartNumber(), request.getEndNumber())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Invalid number range");
        }

        for (ExtensionPool existing : poolRepository.findByOrgId(request.getOrgId())) {
            if (rangesOverlap(existing.getStartNumber(), existing.getEndNumber(),
                    request.getStartNumber(), request.getEndNumber())) {
                throw new BusinessException(ErrorCode.POOL_001);
            }
        }

        ExtensionPool pool = new ExtensionPool();
        pool.setOrgId(request.getOrgId());
        pool.setStartNumber(request.getStartNumber());
        pool.setEndNumber(request.getEndNumber());
        pool.setAllocatedBy(operator);
        pool.setCreatedBy(operator);
        pool.setUpdatedBy(operator);

        return poolRepository.save(pool);
    }

    @Transactional
    public void deletePool(Long id) {
        poolRepository.findById(id).ifPresent(e -> { e.setDeletedAt(LocalDateTime.now()); poolRepository.save(e); });
    }

    @Transactional(readOnly = true)
    public boolean checkOverlap(Long orgId, String startNumber, String endNumber) {
        if (!isValidRange(startNumber, endNumber)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Invalid number range");
        }
        
        for (ExtensionPool existing : poolRepository.findByOrgId(orgId)) {
            if (rangesOverlap(existing.getStartNumber(), existing.getEndNumber(), startNumber, endNumber)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public ExtensionPool findOverlappingPool(Long orgId, String startNumber, String endNumber) {
        for (ExtensionPool existing : poolRepository.findByOrgId(orgId)) {
            if (rangesOverlap(existing.getStartNumber(), existing.getEndNumber(), startNumber, endNumber)) {
                return existing;
            }
        }
        return null;
    }

    private static final double WARNING_THRESHOLD = 80.0;
    private static final double DANGER_THRESHOLD = 90.0;

    @Transactional(readOnly = true)
    public ExtensionPoolUsage getPoolUsage(Long poolId) {
        ExtensionPool pool = getPoolById(poolId);

        int totalCount = calculateRangeCount(pool.getStartNumber(), pool.getEndNumber());
        long usedCount = phoneRepository.findByOrgId(pool.getOrgId()).stream()
                .filter(p -> p.getExtensionNumber() != null)
                .filter(p -> isInRange(p.getExtensionNumber(), pool.getStartNumber(), pool.getEndNumber()))
                .count();

        int idleCount = totalCount - (int) usedCount;
        double usageRate = totalCount > 0 ? (double) usedCount / totalCount * 100 : 0;

        String status;
        String warningMessage = null;
        if (usageRate >= DANGER_THRESHOLD) {
            status = "red";
            warningMessage = "号池即将耗尽，使用率已达" + String.format("%.1f", usageRate) + "%";
        } else if (usageRate >= WARNING_THRESHOLD) {
            status = "yellow";
            warningMessage = "号池使用率较高，已达" + String.format("%.1f", usageRate) + "%";
        } else {
            status = "green";
        }

        ExtensionPoolUsage usage = new ExtensionPoolUsage(poolId, totalCount, (int) usedCount, idleCount, usageRate, status, warningMessage);
        
        if (warningMessage != null) {
            log.warn("Extension pool {} warning: {}", poolId, warningMessage);
        }
        
        return usage;
    }

    @Transactional(readOnly = true)
    public List<ExtensionPoolUsage> getAllPoolUsages() {
        return poolRepository.findAll().stream()
                .map(pool -> getPoolUsage(pool.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExtensionPoolUsage> getWarningPools() {
        return getAllPoolUsages().stream()
                .filter(u -> "yellow".equals(u.status()) || "red".equals(u.status()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> suggestAlternatePools(Long orgId) {
        List<String> suggestions = new java.util.ArrayList<>();
        
        for (ExtensionPoolUsage usage : getAllPoolUsages().stream()
                .filter(u -> u.status().equals("green"))
                .toList()) {
            ExtensionPool pool = getPoolById(usage.poolId());
            if (pool.getOrgId().equals(orgId) && usage.idleCount() > 0) {
                suggestions.add(String.format("Pool %d (%s-%s): %d available", 
                        usage.poolId(), pool.getStartNumber(), pool.getEndNumber(), usage.idleCount()));
            }
        }
        
        return suggestions;
    }

    @Transactional(readOnly = true)
    public PoolExhaustionInfo checkExhaustion(Long poolId) {
        ExtensionPoolUsage usage = getPoolUsage(poolId);
        ExtensionPool pool = getPoolById(poolId);
        
        boolean isExhausted = usage.idleCount() == 0;
        boolean isNearExhausted = usage.usageRate() >= WARNING_THRESHOLD;
        
        List<String> suggestions = isNearExhausted || isExhausted ? suggestAlternatePools(pool.getOrgId()) : List.of();
        
        return new PoolExhaustionInfo(poolId, isExhausted, isNearExhausted, usage.idleCount(), 
                (int) usage.usedCount(), usage.totalCount(), suggestions);
    }

    public record PoolExhaustionInfo(
            Long poolId,
            boolean isExhausted,
            boolean isNearExhausted,
            int idleCount,
            int usedCount,
            int totalCount,
            List<String> suggestions
    ) {}

    private boolean isValidRange(String start, String end) {
        try {
            int startNum = Integer.parseInt(start);
            int endNum = Integer.parseInt(end);
            return startNum <= endNum && startNum > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean rangesOverlap(String start1, String end1, String start2, String end2) {
        int e1 = Integer.parseInt(end1);
        int s2 = Integer.parseInt(start2);
        int e2 = Integer.parseInt(end2);
        int s1 = Integer.parseInt(start1);
        return !(e1 < s2 || e2 < s1);
    }

    private int calculateRangeCount(String start, String end) {
        return Integer.parseInt(end) - Integer.parseInt(start) + 1;
    }

    private boolean isInRange(String number, String start, String end) {
        try {
            int num = Integer.parseInt(number);
            int startNum = Integer.parseInt(start);
            int endNum = Integer.parseInt(end);
            return num >= startNum && num <= endNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public record ExtensionPoolUsage(
            Long poolId,
            int totalCount,
            int usedCount,
            int idleCount,
            double usageRate,
            String status,
            String warningMessage
    ) {}
}

