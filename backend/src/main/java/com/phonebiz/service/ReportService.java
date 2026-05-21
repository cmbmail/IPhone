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

    private final PhoneNumberRepository phoneNumberRepository;
    private final PhoneSnapshotRepository phoneSnapshotRepository;
    private final BillAllocationRepository billAllocationRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository workOrderItemRepository;
    private final OrgStructureRepository orgStructureRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getPhoneAssetReport(String billMonth) {
        Map<String, Object> report = new HashMap<>();

        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findBySnapshotMonth(billMonth);
        
        long totalPhones = snapshots.size();
        long allocatedPhones = snapshots.stream()
                .filter(s -> s.getEmployeeNo() != null && !s.getEmployeeNo().isEmpty())
                .count();
        long idlePhones = snapshots.stream()
                .filter(s -> s.getEmployeeNo() == null || s.getEmployeeNo().isEmpty())
                .count();

        Map<String, Long> statusDistribution = snapshots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatus() != null ? s.getStatus() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Object> orgDistribution = new HashMap<>();
        Map<Long, Long> orgPhoneCount = snapshots.stream()
                .filter(s -> s.getOrgId() != null)
                .collect(Collectors.groupingBy(PhoneSnapshot::getOrgId, Collectors.counting()));
        
        for (Map.Entry<Long, Long> entry : orgPhoneCount.entrySet()) {
            orgStructureRepository.findById(entry.getKey()).ifPresent(org -> {
                orgDistribution.put(org.getName(), entry.getValue());
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
        Map<String, Object> report = new HashMap<>();

        List<BillAllocation> allocations = billAllocationRepository.findByBillMonth(billMonth);
        
        BigDecimal totalAmount = allocations.stream()
                .map(BillAllocation::getChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal anomalyAmount = allocations.stream()
                .filter(BillAllocation::getAnomalyFlag)
                .map(BillAllocation::getChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long anomalyCount = allocations.stream()
                .filter(BillAllocation::getAnomalyFlag)
                .count();

        Map<String, Object> orgAllocation = new HashMap<>();
        Map<Long, BigDecimal> orgAmountMap = allocations.stream()
                .filter(a -> a.getSnapshotOrgId() != null && a.getChargeAmount() != null)
                .collect(Collectors.groupingBy(
                        BillAllocation::getSnapshotOrgId,
                        Collectors.mapping(BillAllocation::getChargeAmount, 
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        for (Map.Entry<Long, BigDecimal> entry : orgAmountMap.entrySet()) {
            orgStructureRepository.findById(entry.getKey()).ifPresent(org -> {
                orgAllocation.put(org.getName(), entry.getValue());
            });
        }

        Map<String, Long> confirmStatus = new HashMap<>();
        confirmStatus.put("pending", allocations.stream()
                .filter(a -> a.getAdminConfirmOrg() == BillAllocation.ConfirmStatus.pending)
                .count());
        confirmStatus.put("correct", allocations.stream()
                .filter(a -> a.getAdminConfirmOrg() == BillAllocation.ConfirmStatus.correct)
                .count());
        confirmStatus.put("wrong", allocations.stream()
                .filter(a -> a.getAdminConfirmOrg() == BillAllocation.ConfirmStatus.wrong)
                .count());

        report.put("billMonth", billMonth);
        report.put("totalRecords", allocations.size());
        report.put("totalAmount", totalAmount);
        report.put("anomalyCount", anomalyCount);
        report.put("anomalyAmount", anomalyAmount);
        report.put("orgAllocation", orgAllocation);
        report.put("confirmStatus", confirmStatus);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkOrderReport(String startTime, String endTime) {
        Map<String, Object> report = new HashMap<>();

        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);

        List<WorkOrder> orders = workOrderRepository.findByCreatedAtBetween(start, end);
        List<WorkOrderItem> items = workOrderItemRepository.findByCreatedAtBetween(start, end);

        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.COMPLETED)
                .count();
        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.PENDING)
                .count();

        Map<String, Long> typeDistribution = items.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getItemType() != null ? i.getItemType().name() : "unknown",
                        Collectors.counting()
                ));

        Map<String, Long> statusDistribution = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus() != null ? o.getStatus().name() : "unknown",
                        Collectors.counting()
                ));

        report.put("startTime", startTime);
        report.put("endTime", endTime);
        report.put("totalOrders", totalOrders);
        report.put("completedOrders", completedOrders);
        report.put("pendingOrders", pendingOrders);
        report.put("completionRate", totalOrders > 0 ? 
                Math.round(completedOrders * 10000.0 / totalOrders) / 100.0 : 0);
        report.put("totalItems", items.size());
        report.put("typeDistribution", typeDistribution);
        report.put("statusDistribution", statusDistribution);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnomalyBillReport(String billMonth) {
        Map<String, Object> report = new HashMap<>();

        List<BillAllocation> anomalies = billAllocationRepository.findByBillMonth(billMonth).stream()
                .filter(BillAllocation::getAnomalyFlag)
                .toList();

        Map<String, Long> anomalyReasonDistribution = anomalies.stream()
                .filter(a -> a.getAnomalyReason() != null)
                .collect(Collectors.groupingBy(
                        a -> categorizeAnomalyReason(a.getAnomalyReason()),
                        Collectors.counting()
                ));

        BigDecimal totalAnomalyAmount = anomalies.stream()
                .map(BillAllocation::getChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> anomalyDetails = anomalies.stream()
                .map(a -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", a.getId());
                    detail.put("phoneNumber", a.getPhoneNumber());
                    detail.put("chargeAmount", a.getChargeAmount());
                    detail.put("anomalyReason", a.getAnomalyReason());
                    detail.put("snapshotOrgName", a.getSnapshotOrgName());
                    detail.put("createdAt", a.getCreatedAt());
                    return detail;
                })
                .collect(Collectors.toList());

        report.put("billMonth", billMonth);
        report.put("totalAnomalies", anomalies.size());
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