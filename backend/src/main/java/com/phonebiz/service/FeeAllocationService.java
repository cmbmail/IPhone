package com.phonebiz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.FeeAllocationDTO;
import com.phonebiz.dto.FeeAllocationDTO.LevelResponse;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.entity.FeeAllocation;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.repository.FeeAllocationRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneSnapshotRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeAllocationService {

    private final FeeAllocationRepository feeAllocationRepository;
    private final PhoneSnapshotRepository phoneSnapshotRepository;
    private final BillRawRepository billRawRepository;
    private final OrgStructureRepository orgStructureRepository;

    // ==================== Calculate + Save ====================

    /**
     * Calculate and persist fee allocations for a given bill month and level.
     * Level 1: 总行→一级分行
     * Level 2: 一级分行→二级分行/部门/综合支行 (direct children)
     * Level 3: 二级分行→部门/综合支行/零专支行 (direct children)
     */
    @Transactional
    public LevelResponse calculateAndSave(String billMonth, int level) {
        // Delete existing allocations for this month+level
        if (feeAllocationRepository.existsByBillMonthAndAllocationLevel(billMonth, level)) {
            feeAllocationRepository.deleteByBillMonthAndLevel(billMonth, level);
        }

        // 1. Build org tree
        List<OrgStructure> allOrgs = orgStructureRepository.findAll();
        Map<Long, OrgStructure> orgMap = allOrgs.stream()
                .collect(Collectors.toMap(OrgStructure::getId, o -> o));

        // 2. Load snapshots
        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findByBillMonth(billMonth);
        if (snapshots.isEmpty()) {
            snapshots = phoneSnapshotRepository.findBySnapshotMonth(billMonth);
        }

        // 3. Build phone→org chain map
        // For each phone, determine its target org at each allocation level
        Map<String, PhoneAllocationInfo> phoneInfoMap = new HashMap<>();
        for (PhoneSnapshot snap : snapshots) {
            Long orgId = snap.getOrgId();
            if (orgId == null) continue;

            // Trace up the org tree to find allocation-level targets
            // Level1: find the TOP-LEVEL branch (depthBelowRoot=1, i.e. direct child of root)
            Long level1OrgId = findAncestorAtDepth(orgId, orgMap, 1);
            Long level2OrgId = findAncestorAtDepth(orgId, orgMap, 2); // direct child of一级分行
            Long level3OrgId = null;
            // Level3 only exists if level2Org is a二级分行 (type=2)
            if (level2OrgId != null) {
                OrgStructure level2Org = orgMap.get(level2OrgId);
                if (level2Org != null && level2Org.getType() == OrgStructure.ORG_BRANCH && level2Org.getLevel() >= 2) {
                    level3OrgId = findAncestorAtDepth(orgId, orgMap, 3); // direct child of二级分行
                }
            }

            phoneInfoMap.put(snap.getPhoneNumber(), new PhoneAllocationInfo(
                    level1OrgId, level2OrgId, level3OrgId, orgId));
        }

        // 4. Load bill_raw
        List<BillRaw> rawBills = billRawRepository.findByBillMonth(billMonth);

        // 5. Aggregate by target org for the requested level
        Map<Long, FeeAggregator> aggMap = new LinkedHashMap<>();
        Map<Long, Long> targetToParentMap = new HashMap<>(); // targetOrgId → parentOrgId
        Map<Long, String> targetNameMap = new HashMap<>();
        Map<Long, Integer> targetTypeMap = new HashMap<>();
        Map<Long, String> parentNameMap = new HashMap<>();

        for (BillRaw raw : rawBills) {
            PhoneAllocationInfo info = phoneInfoMap.get(raw.getPhoneNumber());
            Long targetOrgId = switch (level) {
                case FeeAllocation.LEVEL_1 -> (info != null ? info.level1OrgId : null);
                case FeeAllocation.LEVEL_2 -> (info != null ? info.level2OrgId : null);
                case FeeAllocation.LEVEL_3 -> (info != null ? info.level3OrgId : null);
                default -> null;
            };

            if (targetOrgId == null) {
                targetOrgId = -1L; // unassigned bucket
            }

            FeeAggregator agg = aggMap.computeIfAbsent(targetOrgId, k -> new FeeAggregator());
            BigDecimal chargeAmount = raw.getChargeAmount() != null ? raw.getChargeAmount() : BigDecimal.ZERO;
            agg.totalAmount = agg.totalAmount.add(chargeAmount);
            agg.billCount++;

            int chargeType = raw.getChargeType() != null ? raw.getChargeType() : 0;
            if (chargeType == BillRaw.CHARGE_TYPE_PHONE) {
                agg.platformUsageFee = agg.platformUsageFee.add(
                        raw.getPlatformUsageFee() != null ? raw.getPlatformUsageFee() : BigDecimal.ZERO);
                agg.numberMonthlyRent = agg.numberMonthlyRent.add(
                        raw.getNumberMonthlyRent() != null ? raw.getNumberMonthlyRent() : BigDecimal.ZERO);
                agg.domesticCharge = agg.domesticCharge.add(
                        raw.getDomesticCharge() != null ? raw.getDomesticCharge() : BigDecimal.ZERO);
                agg.internationalCharge = agg.internationalCharge.add(
                        raw.getInternationalCharge() != null ? raw.getInternationalCharge() : BigDecimal.ZERO);
            } else if (chargeType == BillRaw.CHARGE_TYPE_RECORDING) {
                agg.recordingFee = agg.recordingFee.add(chargeAmount);
            } else if (chargeType == BillRaw.CHARGE_TYPE_RINGTONE) {
                agg.ringtoneFee = agg.ringtoneFee.add(chargeAmount);
            } else if (chargeType == BillRaw.CHARGE_TYPE_FLASH_SMS) {
                agg.flashSmsFee = agg.flashSmsFee.add(chargeAmount);
            }
        }

        // Count phones per target org from snapshots
        for (PhoneSnapshot snap : snapshots) {
            PhoneAllocationInfo info = phoneInfoMap.get(snap.getPhoneNumber());
            Long targetOrgId = switch (level) {
                case FeeAllocation.LEVEL_1 -> (info != null ? info.level1OrgId : null);
                case FeeAllocation.LEVEL_2 -> (info != null ? info.level2OrgId : null);
                case FeeAllocation.LEVEL_3 -> (info != null ? info.level3OrgId : null);
                default -> null;
            };
            if (targetOrgId == null) targetOrgId = -1L;
            FeeAggregator agg = aggMap.get(targetOrgId);
            if (agg != null) agg.phoneCount++;
        }

        // 6. Build parent info
        for (Long targetOrgId : aggMap.keySet()) {
            if (targetOrgId == -1L) {
                targetToParentMap.put(targetOrgId, 1L);
                targetNameMap.put(targetOrgId, "未归属");
                targetTypeMap.put(targetOrgId, 0);
                parentNameMap.put(targetOrgId, "招商银行");
                continue;
            }
            OrgStructure targetOrg = orgMap.get(targetOrgId);
            if (targetOrg != null) {
                targetNameMap.put(targetOrgId, targetOrg.getName());
                targetTypeMap.put(targetOrgId, targetOrg.getType());
                Long parentOrgId = targetOrg.getParentId();
                targetToParentMap.put(targetOrgId, parentOrgId);
                OrgStructure parentOrg = orgMap.get(parentOrgId);
                parentNameMap.put(targetOrgId, parentOrg != null ? parentOrg.getName() : "招商银行");
            }
        }

        // 7. Calculate grand total for percentage
        BigDecimal grandTotal = aggMap.values().stream()
                .map(a -> a.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Save to database
        List<FeeAllocation> entities = new ArrayList<>();
        for (Map.Entry<Long, FeeAggregator> entry : aggMap.entrySet()) {
            Long targetOrgId = entry.getKey();
            FeeAggregator agg = entry.getValue();

            BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? agg.totalAmount.multiply(new BigDecimal("100"))
                        .divide(grandTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            entities.add(FeeAllocation.builder()
                    .billMonth(billMonth)
                    .allocationLevel(level)
                    .parentOrgId(targetToParentMap.getOrDefault(targetOrgId, 1L))
                    .parentOrgName(parentNameMap.getOrDefault(targetOrgId, "招商银行"))
                    .orgId(targetOrgId)
                    .orgName(targetNameMap.getOrDefault(targetOrgId, "未知"))
                    .orgType(targetTypeMap.getOrDefault(targetOrgId, 0))
                    .phoneCount(agg.phoneCount)
                    .platformUsageFee(agg.platformUsageFee)
                    .numberMonthlyRent(agg.numberMonthlyRent)
                    .domesticCharge(agg.domesticCharge)
                    .internationalCharge(agg.internationalCharge)
                    .recordingFee(agg.recordingFee)
                    .ringtoneFee(agg.ringtoneFee)
                    .flashSmsFee(agg.flashSmsFee)
                    .totalAmount(agg.totalAmount)
                    .percentage(pct)
                    .status(FeeAllocation.STATUS_PENDING)
                    .build());
        }
        feeAllocationRepository.saveAll(entities);
        log.info("Fee allocation level {} for {}: {} records saved", level, billMonth, entities.size());

        return buildLevelResponse(billMonth, level, entities, grandTotal);
    }

    // ==================== Query ====================

    @Transactional(readOnly = true)
    public LevelResponse getAllocationLevel(String billMonth, int level) {
        List<FeeAllocation> allocations = feeAllocationRepository
                .findByBillMonthAndAllocationLevelOrderByTotalAmountDesc(billMonth, level);

        BigDecimal grandTotal = allocations.stream()
                .map(FeeAllocation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return buildLevelResponse(billMonth, level, allocations, grandTotal);
    }

    @Transactional(readOnly = true)
    public LevelResponse getAllocationByParent(String billMonth, int level, Long parentOrgId) {
        List<FeeAllocation> allocations = feeAllocationRepository
                .findByBillMonthAndParentOrgIdAndAllocationLevelOrderByTotalAmountDesc(
                        billMonth, parentOrgId, level);

        BigDecimal parentTotal = allocations.stream()
                .map(FeeAllocation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return buildLevelResponse(billMonth, level, allocations, parentTotal);
    }

    @Transactional(readOnly = true)
    public List<Long> getParentOrgIds(String billMonth, int level) {
        return feeAllocationRepository.findDistinctParentOrgIds(billMonth, level);
    }

    // ==================== Confirm / Reject ====================

    @Transactional
    public FeeAllocationDTO confirmAllocation(Long id, String confirmedBy) {
        FeeAllocation fa = feeAllocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));
        fa.setStatus(FeeAllocation.STATUS_CONFIRMED);
        fa.setConfirmedBy(confirmedBy);
        fa.setConfirmedAt(LocalDateTime.now());
        feeAllocationRepository.save(fa);
        return FeeAllocationDTO.from(fa);
    }

    @Transactional
    public FeeAllocationDTO rejectAllocation(Long id, String rejectedBy) {
        FeeAllocation fa = feeAllocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));
        fa.setStatus(FeeAllocation.STATUS_REJECTED);
        fa.setConfirmedBy(rejectedBy);
        fa.setConfirmedAt(LocalDateTime.now());
        feeAllocationRepository.save(fa);
        return FeeAllocationDTO.from(fa);
    }

    // ==================== Helpers ====================

    /**
     * Find the ancestor of orgId at a given depth below root.
     * depthBelowRoot=1 → 一级分行 (child of root)
     * depthBelowRoot=2 → direct child of一级分行
     * depthBelowRoot=3 → direct child of二级分行
     */
    private Long findAncestorAtDepth(Long orgId, Map<Long, OrgStructure> orgMap, int depthBelowRoot) {
        OrgStructure org = orgMap.get(orgId);
        if (org == null) return null;

        // Walk up the tree building the path
        List<Long> path = new ArrayList<>();
        Long currentId = orgId;
        while (currentId != null) {
            path.add(0, currentId); // prepend
            OrgStructure current = orgMap.get(currentId);
            if (current == null) break;
            currentId = current.getParentId();
        }

        // path[0] = root (集团), path[1] = 一级分行, path[2] = direct child of一级分行, ...
        if (path.size() > depthBelowRoot) {
            return path.get(depthBelowRoot);
        }
        // org is at or above the target depth
        return orgId;
    }

    private LevelResponse buildLevelResponse(String billMonth, int level,
                                              List<FeeAllocation> allocations, BigDecimal grandTotal) {
        BigDecimal totalPlatformUsage = BigDecimal.ZERO;
        BigDecimal totalMonthlyRent = BigDecimal.ZERO;
        BigDecimal totalDomestic = BigDecimal.ZERO;
        BigDecimal totalInternational = BigDecimal.ZERO;
        BigDecimal totalRecording = BigDecimal.ZERO;
        BigDecimal totalRingtone = BigDecimal.ZERO;
        BigDecimal totalFlash = BigDecimal.ZERO;
        int totalPhones = 0;

        List<FeeAllocationDTO> items = new ArrayList<>();
        for (FeeAllocation fa : allocations) {
            items.add(FeeAllocationDTO.from(fa));
            totalPlatformUsage = totalPlatformUsage.add(fa.getPlatformUsageFee());
            totalMonthlyRent = totalMonthlyRent.add(fa.getNumberMonthlyRent());
            totalDomestic = totalDomestic.add(fa.getDomesticCharge());
            totalInternational = totalInternational.add(fa.getInternationalCharge());
            totalRecording = totalRecording.add(fa.getRecordingFee());
            totalRingtone = totalRingtone.add(fa.getRingtoneFee());
            totalFlash = totalFlash.add(fa.getFlashSmsFee());
            totalPhones += fa.getPhoneCount();
        }

        String levelName = switch (level) {
            case 1 -> "一次分摊";
            case 2 -> "二次分摊";
            case 3 -> "三次分摊";
            default -> "未知";
        };

        String levelDesc = switch (level) {
            case 1 -> "总行 → 一级分行";
            case 2 -> "一级分行 → 二级分行/部门/综合支行";
            case 3 -> "二级分行 → 部门/综合支行/零专支行";
            default -> "";
        };

        return LevelResponse.builder()
                .billMonth(billMonth)
                .allocationLevel(level)
                .levelName(levelName)
                .levelDescription(levelDesc)
                .totalCount(allocations.size())
                .totalPhones(totalPhones)
                .totalAmount(grandTotal)
                .totalPlatformUsageFee(totalPlatformUsage)
                .totalNumberMonthlyRent(totalMonthlyRent)
                .totalDomesticCharge(totalDomestic)
                .totalInternationalCharge(totalInternational)
                .totalRecordingFee(totalRecording)
                .totalRingtoneFee(totalRingtone)
                .totalFlashSmsFee(totalFlash)
                .calculated(!allocations.isEmpty())
                .items(items)
                .build();
    }

    // Inner classes for aggregation
    private static class PhoneAllocationInfo {
        Long level1OrgId; // 一级分行
        Long level2OrgId; // direct child of一级分行
        Long level3OrgId; // direct child of二级分行
        Long orgId;       // actual org

        PhoneAllocationInfo(Long l1, Long l2, Long l3, Long orgId) {
            this.level1OrgId = l1;
            this.level2OrgId = l2;
            this.level3OrgId = l3;
            this.orgId = orgId;
        }
    }

    private static class FeeAggregator {
        int phoneCount = 0;
        int billCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal platformUsageFee = BigDecimal.ZERO;
        BigDecimal numberMonthlyRent = BigDecimal.ZERO;
        BigDecimal domesticCharge = BigDecimal.ZERO;
        BigDecimal internationalCharge = BigDecimal.ZERO;
        BigDecimal recordingFee = BigDecimal.ZERO;
        BigDecimal ringtoneFee = BigDecimal.ZERO;
        BigDecimal flashSmsFee = BigDecimal.ZERO;
    }
}
