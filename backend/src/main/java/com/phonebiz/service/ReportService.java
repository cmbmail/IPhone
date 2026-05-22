package com.phonebiz.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.entity.*;
import com.phonebiz.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PhoneSnapshotRepository phoneSnapshotRepository;
    private final BillAllocationRepository billAllocationRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository workOrderItemRepository;
    private final OrgStructureRepository orgStructureRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getPhoneAssetReport(String billMonth) {
        Map<String, Object> report = new LinkedHashMap<>();

        long totalPhones = phoneSnapshotRepository.countByMonth(billMonth);
        long allocatedPhones = phoneSnapshotRepository.countAllocatedByMonth(billMonth);
        long idlePhones = phoneSnapshotRepository.countIdleByMonth(billMonth);

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (Object[] row : phoneSnapshotRepository.countGroupByStatus(billMonth)) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status != null ? status : "unknown", count);
        }

        Map<String, Object> orgDistribution = new LinkedHashMap<>();
        Map<Long, Long> orgPhoneCount = new LinkedHashMap<>();
        for (Object[] row : phoneSnapshotRepository.countGroupByOrgId(billMonth)) {
            Long orgId = (Long) row[0];
            Long count = (Long) row[1];
            orgPhoneCount.put(orgId, count);
        }
        // Batch load org names
        if (!orgPhoneCount.isEmpty()) {
            orgStructureRepository.findAllById(orgPhoneCount.keySet()).forEach(org -> {
                orgDistribution.put(org.getName(), orgPhoneCount.get(org.getId()));
            });
        }

        report.put("billMonth", billMonth);
        report.put("totalPhones", totalPhones);
        report.put("allocatedPhones", allocatedPhones);
        report.put("idlePhones", idlePhones);
        report.put("allocationRate", totalPhones > 0 ?
                Math.round(allocatedPhones * 10000.0 / totalPhones) / 100.0 : 0);
        report.put("statusDistribution", statusDistribution);
        report.put("orgDistribution", orgDistribution);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBillAllocationReport(String billMonth) {
        Map<String, Object> report = new LinkedHashMap<>();

        int totalRecords = billAllocationRepository.countByBillMonth(billMonth);
        BigDecimal totalAmount = billAllocationRepository.sumChargeAmountByMonth(billMonth);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        int anomalyCount = billAllocationRepository.countByBillMonthAndAnomalyFlag(billMonth, true);
        BigDecimal anomalyAmount = billAllocationRepository.sumAnomalyAmountByMonth(billMonth);
        if (anomalyAmount == null) anomalyAmount = BigDecimal.ZERO;

        // Org allocation via JPQL GROUP BY (returns orgId, sum(amount), count)
        Map<String, Object> orgAllocation = new LinkedHashMap<>();
        Map<Long, BigDecimal> orgAmountMap = new LinkedHashMap<>();
        for (Object[] row : billAllocationRepository.sumAndCountGroupByOrgId(billMonth)) {
            Long orgId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            orgAmountMap.put(orgId, amount);
        }
        if (!orgAmountMap.isEmpty()) {
            orgStructureRepository.findAllById(orgAmountMap.keySet()).forEach(org -> {
                orgAllocation.put(org.getName(), orgAmountMap.get(org.getId()));
            });
        }

        // Confirm status group by
        Map<String, Long> confirmStatus = new LinkedHashMap<>();
        for (Object[] row : billAllocationRepository.countByConfirmStatusGroupBy(billMonth)) {
            BillAllocation.ConfirmStatus status = (BillAllocation.ConfirmStatus) row[0];
            Long count = (Long) row[1];
            confirmStatus.put(status.name(), count);
        }

        report.put("billMonth", billMonth);
        report.put("totalRecords", totalRecords);
        report.put("totalAmount", totalAmount);
        report.put("anomalyCount", anomalyCount);
        report.put("anomalyAmount", anomalyAmount);
        report.put("orgAllocation", orgAllocation);
        report.put("confirmStatus", confirmStatus);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkOrderReport(String startTime, String endTime) {
        Map<String, Object> report = new LinkedHashMap<>();

        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);

        long totalOrders = workOrderRepository.countBetween(start, end);
        long totalItems = workOrderItemRepository.countBetween(start, end);

        // Status distribution via JPQL
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        long completedOrders = 0;
        long pendingOrders = 0;
        for (Object[] row : workOrderRepository.countGroupByStatusBetween(start, end)) {
            WorkOrder.WorkOrderStatus status = (WorkOrder.WorkOrderStatus) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status.name(), count);
            if (status == WorkOrder.WorkOrderStatus.COMPLETED) completedOrders = count;
            if (status == WorkOrder.WorkOrderStatus.PENDING) pendingOrders = count;
        }

        // Item type distribution via JPQL
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Object[] row : workOrderItemRepository.countGroupByTypeBetween(start, end)) {
            WorkOrderItem.ItemType type = (WorkOrderItem.ItemType) row[0];
            Long count = (Long) row[1];
            typeDistribution.put(type != null ? type.name() : "unknown", count);
        }

        report.put("startTime", startTime);
        report.put("endTime", endTime);
        report.put("totalOrders", totalOrders);
        report.put("completedOrders", completedOrders);
        report.put("pendingOrders", pendingOrders);
        report.put("completionRate", totalOrders > 0 ?
                Math.round(completedOrders * 10000.0 / totalOrders) / 100.0 : 0);
        report.put("totalItems", totalItems);
        report.put("typeDistribution", typeDistribution);
        report.put("statusDistribution", statusDistribution);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnomalyBillReport(String billMonth) {
        Map<String, Object> report = new LinkedHashMap<>();

        int totalAnomalies = billAllocationRepository.countByBillMonthAndAnomalyFlag(billMonth, true);
        BigDecimal totalAnomalyAmount = billAllocationRepository.sumAnomalyChargeByMonth(billMonth);
        if (totalAnomalyAmount == null) totalAnomalyAmount = BigDecimal.ZERO;

        // Anomaly reason distribution via JPQL
        Map<String, Long> anomalyReasonDistribution = new LinkedHashMap<>();
        for (Object[] row : billAllocationRepository.countAnomalyByReason(billMonth)) {
            String reason = (String) row[0];
            Long count = (Long) row[1];
            anomalyReasonDistribution.put(categorizeAnomalyReason(reason), count);
        }

        // Anomaly details via projection query (only selected columns, not full entity)
        List<Map<String, Object>> anomalyDetails = new ArrayList<>();
        for (Object[] row : billAllocationRepository.findAnomalyProjectionByMonth(billMonth)) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("id", row[0]);
            detail.put("phoneNumber", row[1]);
            detail.put("chargeAmount", row[2]);
            detail.put("anomalyReason", row[3]);
            detail.put("snapshotOrgName", row[4]);
            detail.put("createdAt", row[5]);
            anomalyDetails.add(detail);
        }

        report.put("billMonth", billMonth);
        report.put("totalAnomalies", totalAnomalies);
        report.put("totalAnomalyAmount", totalAnomalyAmount);
        report.put("anomalyReasonDistribution", anomalyReasonDistribution);
        report.put("anomalyDetails", anomalyDetails);

        return report;
    }

    private String categorizeAnomalyReason(String reason) {
        if (reason.contains("无快照")) {
            return "无快照匹配";
        } else if (reason.contains("50%")) {
            return "金额波动过大";
        } else if (reason.contains("5σ")) {
            return "异常大额";
        } else {
            return "其他异常";
        }
    }

    @Transactional(readOnly = true)
    public Page<BillAllocation> getAnomalyBillPage(String billMonth, Pageable pageable) {
        return billAllocationRepository.findByBillMonthAndAnomalyFlag(billMonth, true, pageable);
    }
}
