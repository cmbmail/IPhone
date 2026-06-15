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
        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findBySnapshotMonth(billMonth);
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
}