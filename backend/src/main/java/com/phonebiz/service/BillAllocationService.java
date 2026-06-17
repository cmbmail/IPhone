package com.phonebiz.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.repository.BillAllocationRepository;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.dto.BillAllocationDTO;
import com.phonebiz.dto.BillAllocationSummaryDTO;
import com.phonebiz.dto.BranchAllocationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.phonebiz.repository.PhoneSnapshotRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillAllocationService {

    private final com.phonebiz.security.DataScope dataScope;

    private final BillAllocationRepository billAllocationRepository;
    private final BillRawRepository billRawRepository;
    private final PhoneSnapshotRepository phoneSnapshotRepository;
    private final CostCenterMappingRepository costCenterMappingRepository;

    private static final BigDecimal ANOMALY_THRESHOLD = new BigDecimal("0.5");
    private static final BigDecimal FIVE_SIGMA_MULTIPLIER = new BigDecimal("5");

    @Transactional
    public void autoAllocateAndSave(List<BillRaw> rawBills, String billMonth, String operator) {
        // Priority 1: Use snapshots linked to this billMonth via billMonth field
        // Priority 2: Fall back to snapshots by snapshotMonth = billMonth
        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findByBillMonth(billMonth);
        if (snapshots.isEmpty()) {
            snapshots = phoneSnapshotRepository.findBySnapshotMonth(billMonth);
        }
        log.info("Allocation for bill month {}: using {} snapshots (source: {})", 
                billMonth, snapshots.size(), 
                phoneSnapshotRepository.findByBillMonth(billMonth).isEmpty() ? "snapshotMonth" : "billMonth link");
        Map<String, PhoneSnapshot> snapshotMap = new HashMap<>();
        for (PhoneSnapshot snapshot : snapshots) {
            snapshotMap.put(snapshot.getPhoneNumber(), snapshot);
        }

        List<BillAllocation> allocations = new ArrayList<>();
        Map<String, List<BigDecimal>> phoneAmountHistory = loadHistoricalData(rawBills);

        for (BillRaw raw : rawBills) {
            BillAllocation allocation = createAllocation(raw, snapshotMap, phoneAmountHistory);
            allocations.add(allocation);
        }

        billAllocationRepository.saveAll(allocations);
        log.info("Auto allocation completed for month {}: {} records", billMonth, allocations.size());
    }

    private BillAllocation createAllocation(BillRaw raw, Map<String, PhoneSnapshot> snapshotMap,
                                           Map<String, List<BigDecimal>> phoneAmountHistory) {
        BillAllocation allocation = BillAllocation.builder()
                .billMonth(raw.getBillMonth())
                .billRawId(raw.getId())
                .phoneNumber(raw.getPhoneNumber())
                .chargeAmount(raw.getChargeAmount())
                .anomalyFlag(false)
                .adminConfirmOrg(BillAllocation.ConfirmStatus.pending)
                .adminConfirmAmount(BillAllocation.ConfirmStatus.pending)
                .financeConfirmAnomaly(BillAllocation.FinanceConfirmStatus.pending)
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        PhoneSnapshot snapshot = snapshotMap.get(raw.getPhoneNumber());
        if (snapshot != null) {
            allocation.setPhoneId(snapshot.getPhoneId());
            allocation.setSnapshotOrgId(snapshot.getOrgId());
            allocation.setSnapshotOrgName(snapshot.getOrgName());
            allocation.setCostCenterCode(snapshot.getCostCenterCode());
        } else {
            detectAnomaly(allocation, "无快照匹配");
        }

        checkHistoricalAnomaly(allocation, phoneAmountHistory);

        return allocation;
    }

    private void detectAnomaly(BillAllocation allocation, String reason) {
        allocation.setAnomalyFlag(true);
        if (allocation.getAnomalyReason() == null) {
            allocation.setAnomalyReason(reason);
        } else {
            allocation.setAnomalyReason(allocation.getAnomalyReason() + "; " + reason);
        }
    }

    private void checkHistoricalAnomaly(BillAllocation allocation, Map<String, List<BigDecimal>> phoneAmountHistory) {
        String phoneNumber = allocation.getPhoneNumber();
        List<BigDecimal> history = phoneAmountHistory.get(phoneNumber);

        if (history == null || history.isEmpty()) {
            return;
        }

        BigDecimal currentAmount = allocation.getChargeAmount();
        BigDecimal average = calculateAverage(history);
        BigDecimal standardDeviation = calculateStandardDeviation(history, average);

        if (average.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deviation = currentAmount.subtract(average).abs();
            BigDecimal threshold = average.multiply(ANOMALY_THRESHOLD);

            if (deviation.compareTo(threshold) > 0) {
                detectAnomaly(allocation, "金额与历史月均差异超过50%");
            }
        }

        BigDecimal fiveSigmaThreshold = average.add(standardDeviation.multiply(FIVE_SIGMA_MULTIPLIER));
        if (currentAmount.compareTo(fiveSigmaThreshold) > 0) {
            detectAnomaly(allocation, "金额异常大（超过5σ）");
        }
    }

    private BigDecimal calculateAverage(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumOfSquaredDifferences = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumOfSquaredDifferences.divide(
                new BigDecimal(values.size() - 1), 2, java.math.RoundingMode.HALF_UP);

        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }

    private Map<String, List<BigDecimal>> loadHistoricalData(List<BillRaw> currentBills) {
        Map<String, List<BigDecimal>> history = new HashMap<>();

        Set<String> phoneNumbers = new HashSet<>();
        for (BillRaw raw : currentBills) {
            phoneNumbers.add(raw.getPhoneNumber());
        }

        for (String phoneNumber : phoneNumbers) {
            List<BillAllocation> allocations = findByPhoneNumber(phoneNumber);
            if (!allocations.isEmpty()) {
                List<BigDecimal> amounts = new ArrayList<>();
                for (BillAllocation allocation : allocations) {
                    amounts.add(allocation.getChargeAmount());
                }
                history.put(phoneNumber, amounts);
            }
        }

        return history;
    }

    private List<BillAllocation> findByPhoneNumber(String phoneNumber) {
        return billAllocationRepository.findByPhoneNumber(phoneNumber);
    }

    @Transactional
    public void adminConfirmOrg(Long allocationId, BillAllocation.ConfirmStatus status, String confirmBy) {
        BillAllocation allocation = billAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (allocation.getFinanceConfirmSubmit() == BillAllocation.FinanceSubmitStatus.submitted) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        allocation.setAdminConfirmOrg(status);
        allocation.setAdminConfirmBy(confirmBy);
        allocation.setAdminConfirmAt(LocalDateTime.now());
        billAllocationRepository.save(allocation);
    }

    @Transactional
    public void adminConfirmAmount(Long allocationId, BillAllocation.ConfirmStatus status, String confirmBy) {
        BillAllocation allocation = billAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (allocation.getFinanceConfirmSubmit() == BillAllocation.FinanceSubmitStatus.submitted) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        allocation.setAdminConfirmAmount(status);
        allocation.setAdminConfirmBy(confirmBy);
        allocation.setAdminConfirmAt(LocalDateTime.now());
        billAllocationRepository.save(allocation);
    }

    @Transactional
    public void financeConfirmAnomaly(Long allocationId, BillAllocation.FinanceConfirmStatus status, String confirmBy) {
        BillAllocation allocation = billAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (allocation.getFinanceConfirmSubmit() == BillAllocation.FinanceSubmitStatus.submitted) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        allocation.setFinanceConfirmAnomaly(status);
        allocation.setFinanceConfirmBy(confirmBy);
        allocation.setFinanceConfirmAt(LocalDateTime.now());
        billAllocationRepository.save(allocation);
    }

    @Transactional
    public void financeSubmit(Long allocationId, String submitBy) {
        BillAllocation allocation = billAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (allocation.getFinanceConfirmSubmit() == BillAllocation.FinanceSubmitStatus.submitted) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        allocation.setFinanceConfirmSubmit(BillAllocation.FinanceSubmitStatus.submitted);
        allocation.setFinanceSubmitBy(submitBy);
        allocation.setFinanceSubmitAt(LocalDateTime.now());
        billAllocationRepository.save(allocation);
    }

    @Transactional
    public void rejectAndReset(Long allocationId, String reason) {
        BillAllocation allocation = billAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (allocation.getFinanceConfirmSubmit() == BillAllocation.FinanceSubmitStatus.submitted) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        allocation.setAdminConfirmOrg(BillAllocation.ConfirmStatus.pending);
        allocation.setAdminConfirmAmount(BillAllocation.ConfirmStatus.pending);
        allocation.setFinanceConfirmAnomaly(BillAllocation.FinanceConfirmStatus.pending);
        billAllocationRepository.save(allocation);
    }

    // ==================== Query methods (moved from Controller) ====================

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getAllocations(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonth(billMonth, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getAnomalies(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonthAndAnomalyFlag(billMonth, true, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getPendingOrgConfirm(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonthAndAdminConfirmOrg(billMonth, BillAllocation.ConfirmStatus.pending, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getPendingAmountConfirm(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonthAndAdminConfirmAmount(billMonth, BillAllocation.ConfirmStatus.pending, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getPendingFinanceConfirm(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonthAndFinanceConfirmAnomaly(billMonth, BillAllocation.FinanceConfirmStatus.pending, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BillAllocationDTO> getPendingSubmit(String billMonth, Pageable pageable) {
        Page<BillAllocation> page = billAllocationRepository.findByBillMonthAndFinanceConfirmSubmit(billMonth, BillAllocation.FinanceSubmitStatus.pending, pageable);
        return page.map(BillAllocationDTO::from);
    }

    @Transactional(readOnly = true)
    public java.util.List<BillAllocationSummaryDTO> getAllocationSummary(String billMonth) {
        java.util.List<Object[]> rows = billRawRepository.sumByBranch(billMonth);
        java.util.List<BillAllocationSummaryDTO> result = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String branchName = (String) row[0];
            if (branchName == null || branchName.isEmpty()) {
                branchName = "未分配";
            }
            java.math.BigDecimal platformUsageFee = (java.math.BigDecimal) row[1];
            java.math.BigDecimal numberMonthlyRent = (java.math.BigDecimal) row[2];
            Integer outboundDuration = ((Number) row[3]).intValue();
            Integer transferOutboundDuration = ((Number) row[4]).intValue();
            java.math.BigDecimal domesticCharge = (java.math.BigDecimal) row[5];
            java.math.BigDecimal internationalCharge = (java.math.BigDecimal) row[6];
            java.math.BigDecimal recordingFee = (java.math.BigDecimal) row[7];
            java.math.BigDecimal ringtoneFee = (java.math.BigDecimal) row[8];
            java.math.BigDecimal flashSmsFee = (java.math.BigDecimal) row[9];
            java.math.BigDecimal totalAmount = (java.math.BigDecimal) row[10];
            java.math.BigDecimal feeSubtotal = platformUsageFee.add(numberMonthlyRent).add(domesticCharge).add(internationalCharge);

            result.add(BillAllocationSummaryDTO.builder()
                    .branchName(branchName)
                    .platformUsageFee(platformUsageFee)
                    .numberMonthlyRent(numberMonthlyRent)
                    .outboundDuration(outboundDuration)
                    .transferOutboundDuration(transferOutboundDuration)
                    .domesticCharge(domesticCharge)
                    .internationalCharge(internationalCharge)
                    .feeSubtotal(feeSubtotal)
                    .recordingFee(recordingFee)
                    .ringtoneFee(ringtoneFee)
                    .flashSmsFee(flashSmsFee)
                    .totalAmount(totalAmount)
                    .build());
        }
        return result;
    }

    // ==================== Branch-level Allocation ====================

    /**
     * Generate branch-level allocation summary for a bill month.
     * Uses phone_snapshot to resolve branch for each phone number,
     * then aggregates bill_allocation charges by branch.
     */
    @Transactional(readOnly = true)
    public BranchAllocationDTO.BranchAllocationResponse getBranchAllocation(String billMonth) {
        // 1. Load snapshots: prefer billMonth-linked, fallback to snapshotMonth
        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findByBillMonth(billMonth);
        String snapshotMonth = billMonth;
        if (snapshots.isEmpty()) {
            snapshots = phoneSnapshotRepository.findBySnapshotMonth(billMonth);
        } else {
            snapshotMonth = snapshots.get(0).getSnapshotMonth();
        }

        // 2. Build phone->branch map from snapshots
        Map<String, BranchKey> phoneToBranch = new HashMap<>();
        for (PhoneSnapshot snap : snapshots) {
            Long branchId = snap.getBranchOrgId();
            String branchName = snap.getBranchName();
            if (branchId == null && branchName == null) {
                branchName = "未归属";
            }
            phoneToBranch.put(snap.getPhoneNumber(), new BranchKey(
                branchId != null ? branchId : -1L,
                branchName != null ? branchName : "未归属"
            ));
        }

        // 3. Load bill_raw for fee breakdown
        List<BillRaw> rawBills = billRawRepository.findByBillMonth(billMonth);

        // 4. Aggregate bill_raw by branch (from snapshot)
        Map<BranchKey, BranchAggregator> branchMap = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandPlatformUsage = BigDecimal.ZERO;
        BigDecimal grandMonthlyRent = BigDecimal.ZERO;
        BigDecimal grandDomestic = BigDecimal.ZERO;
        BigDecimal grandInternational = BigDecimal.ZERO;
        BigDecimal grandCall = BigDecimal.ZERO;
        BigDecimal grandRecording = BigDecimal.ZERO;
        BigDecimal grandRingtone = BigDecimal.ZERO;
        BigDecimal grandFlash = BigDecimal.ZERO;

        for (BillRaw raw : rawBills) {
            BranchKey key = phoneToBranch.getOrDefault(raw.getPhoneNumber(), new BranchKey(-1L, "未归属"));
            BranchAggregator agg = branchMap.computeIfAbsent(key, k -> new BranchAggregator());

            BigDecimal chargeAmount = raw.getChargeAmount() != null ? raw.getChargeAmount() : BigDecimal.ZERO;
            agg.totalCharge = agg.totalCharge.add(chargeAmount);
            grandTotal = grandTotal.add(chargeAmount);

            // Fee breakdown by charge_type
            int chargeType = raw.getChargeType() != null ? raw.getChargeType() : 0;
            if (chargeType == BillRaw.CHARGE_TYPE_PHONE) {
                BigDecimal platFee = raw.getPlatformUsageFee() != null ? raw.getPlatformUsageFee() : BigDecimal.ZERO;
                BigDecimal rentFee = raw.getNumberMonthlyRent() != null ? raw.getNumberMonthlyRent() : BigDecimal.ZERO;
                BigDecimal domFee = raw.getDomesticCharge() != null ? raw.getDomesticCharge() : BigDecimal.ZERO;
                BigDecimal intFee = raw.getInternationalCharge() != null ? raw.getInternationalCharge() : BigDecimal.ZERO;
                BigDecimal callFee = BigDecimal.ZERO; // BillRaw has no callAmount field

                agg.platformUsageFee = agg.platformUsageFee.add(platFee);
                agg.numberMonthlyRent = agg.numberMonthlyRent.add(rentFee);
                agg.domesticCharge = agg.domesticCharge.add(domFee);
                agg.internationalCharge = agg.internationalCharge.add(intFee);
                

                grandPlatformUsage = grandPlatformUsage.add(platFee);
                grandMonthlyRent = grandMonthlyRent.add(rentFee);
                grandDomestic = grandDomestic.add(domFee);
                grandInternational = grandInternational.add(intFee);
                

                agg.phoneBillCount++;
            } else if (chargeType == BillRaw.CHARGE_TYPE_RECORDING) {
                agg.recordingFee = agg.recordingFee.add(chargeAmount);
                grandRecording = grandRecording.add(chargeAmount);
            } else if (chargeType == BillRaw.CHARGE_TYPE_RINGTONE) {
                agg.ringtoneFee = agg.ringtoneFee.add(chargeAmount);
                grandRingtone = grandRingtone.add(chargeAmount);
            } else if (chargeType == BillRaw.CHARGE_TYPE_FLASH_SMS) {
                agg.flashSmsFee = agg.flashSmsFee.add(chargeAmount);
                grandFlash = grandFlash.add(chargeAmount);
            }

            agg.totalBillCount++;
        }

        // 5. Count allocation status from bill_allocation
        List<BillAllocation> allocations = billAllocationRepository.findByBillMonth(billMonth);
        for (BillAllocation alloc : allocations) {
            BranchKey key = phoneToBranch.getOrDefault(alloc.getPhoneNumber(), new BranchKey(-1L, "未归属"));
            BranchAggregator agg = branchMap.get(key);
            if (agg != null) {
                agg.phoneCount++;
                if (Boolean.TRUE.equals(alloc.getAnomalyFlag())) {
                    agg.anomalyCount++;
                } else {
                    agg.allocatedCount++;
                }
            }
        }

        // 6. Build DTOs sorted by totalCharge desc
        List<BranchAllocationDTO> branches = new ArrayList<>();
        for (Map.Entry<BranchKey, BranchAggregator> entry : branchMap.entrySet()) {
            BranchKey key = entry.getKey();
            BranchAggregator agg = entry.getValue();
            BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? agg.totalCharge.multiply(new BigDecimal("100")).divide(grandTotal, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            BigDecimal feeSubtotal = agg.platformUsageFee.add(agg.numberMonthlyRent)
                .add(agg.domesticCharge).add(agg.internationalCharge);

            branches.add(BranchAllocationDTO.builder()
                .branchOrgId(key.id == -1L ? null : key.id)
                .branchName(key.name)
                .phoneCount(agg.phoneCount)
                .totalChargeAmount(agg.totalCharge)
                .platformUsageFee(agg.platformUsageFee)
                .numberMonthlyRent(agg.numberMonthlyRent)
                .domesticCharge(agg.domesticCharge)
                .internationalCharge(agg.internationalCharge)
                .callAmount(agg.callAmount)
                .recordingFee(agg.recordingFee)
                .ringtoneFee(agg.ringtoneFee)
                .flashSmsFee(agg.flashSmsFee)
                .feeSubtotal(feeSubtotal)
                .allocatedCount(agg.allocatedCount)
                .anomalyCount(agg.anomalyCount)
                .unallocatedCount(agg.totalBillCount - agg.allocatedCount - agg.anomalyCount)
                .chargePercentage(pct)
                .build());
        }

        branches.sort((a, b) -> b.getTotalChargeAmount().compareTo(a.getTotalChargeAmount()));

        return BranchAllocationDTO.BranchAllocationResponse.builder()
                .billMonth(billMonth)
                .snapshotMonth(snapshotMonth)
                .totalBranches(branches.size())
                .totalPhones(branches.stream().mapToInt(BranchAllocationDTO::getPhoneCount).sum())
                .totalAmount(grandTotal)
                .totalPlatformUsageFee(grandPlatformUsage)
                .totalNumberMonthlyRent(grandMonthlyRent)
                .totalDomesticCharge(grandDomestic)
                .totalInternationalCharge(grandInternational)
                .totalCallAmount(grandCall)
                .totalRecordingFee(grandRecording)
                .totalRingtoneFee(grandRingtone)
                .totalFlashSmsFee(grandFlash)
                .branches(branches)
                .build();
    }

    private static class BranchKey {
        final Long id;
        final String name;
        BranchKey(Long id, String name) { this.id = id; this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BranchKey)) return false;
            BranchKey that = (BranchKey) o;
            return id.equals(that.id);
        }
        @Override public int hashCode() { return id.hashCode(); }
    }

    private static class BranchAggregator {
        int phoneCount = 0;
        int totalBillCount = 0;
        int phoneBillCount = 0;
        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal platformUsageFee = BigDecimal.ZERO;
        BigDecimal numberMonthlyRent = BigDecimal.ZERO;
        BigDecimal domesticCharge = BigDecimal.ZERO;
        BigDecimal internationalCharge = BigDecimal.ZERO;
        BigDecimal callAmount = BigDecimal.ZERO;
        BigDecimal recordingFee = BigDecimal.ZERO;
        BigDecimal ringtoneFee = BigDecimal.ZERO;
        BigDecimal flashSmsFee = BigDecimal.ZERO;
        int allocatedCount = 0;
        int anomalyCount = 0;
    }

}