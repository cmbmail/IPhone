package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateExtensionPoolRequest;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.ExtensionPoolRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        poolRepository.deleteById(id);
    }

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
        if (usageRate >= 90) {
            status = "red";
        } else if (usageRate >= 70) {
            status = "yellow";
        } else {
            status = "green";
        }

        return new ExtensionPoolUsage(poolId, totalCount, (int) usedCount, idleCount, usageRate, status);
    }

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
            String status
    ) {}
}
