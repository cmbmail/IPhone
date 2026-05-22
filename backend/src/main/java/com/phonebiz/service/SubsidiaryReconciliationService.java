package com.phonebiz.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.SubsidiaryReconciliation;
import com.phonebiz.entity.Notification;
import com.phonebiz.repository.BillAllocationRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.SubsidiaryReconciliationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubsidiaryReconciliationService {

    private final SubsidiaryReconciliationRepository reconciliationRepository;
    private final BillAllocationRepository billAllocationRepository;
    private final OrgStructureRepository orgStructureRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional
    public void generateReconciliation(String billMonth) {
        // P1-07: Delete existing reconciliation records for this month before regenerating
        List<SubsidiaryReconciliation> existing = reconciliationRepository.findByBillMonth(billMonth);
        if (!existing.isEmpty()) {
            // Only delete pending ones, keep confirmed records
            List<SubsidiaryReconciliation> pending = existing.stream()
                .filter(r -> r.getReconciliationStatus() == SubsidiaryReconciliation.RECON_PENDING)
                .toList();
            if (!pending.isEmpty()) {
                pending.forEach(e -> e.setDeletedAt(LocalDateTime.now())); reconciliationRepository.saveAll(pending);
                log.info("Deleted {} pending reconciliation records for month {}", pending.size(), billMonth);
            }
        }
        
        List<BillAllocation> allocations = billAllocationRepository.findByBillMonth(billMonth);
        
        Map<Long, BigDecimal> orgAmountMap = allocations.stream()
                .filter(a -> a.getSnapshotOrgId() != null && a.getChargeAmount() != null)
                .collect(Collectors.groupingBy(
                        BillAllocation::getSnapshotOrgId,
                        Collectors.mapping(BillAllocation::getChargeAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        for (Map.Entry<Long, BigDecimal> entry : orgAmountMap.entrySet()) {
            SubsidiaryReconciliation reconciliation = SubsidiaryReconciliation.builder()
                    .billMonth(billMonth)
                    .subsidiaryOrgId(entry.getKey())
                    .totalAmount(entry.getValue())
                    .invoiceCount((int) allocations.stream()
                            .filter(a -> entry.getKey().equals(a.getSnapshotOrgId()))
                            .count())
                    .reconciliationStatus(SubsidiaryReconciliation.RECON_PENDING)
                    .build();

            reconciliationRepository.save(reconciliation);
        }

        log.info("Reconciliation records generated for month {}: {} records", billMonth, orgAmountMap.size());
    }

    @Transactional
    public SubsidiaryReconciliation subsidiaryConfirm(Long reconciliationId, String confirmBy) {
        SubsidiaryReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (reconciliation.getReconciliationStatus() == SubsidiaryReconciliation.RECON_GROUP_CONFIRMED) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        reconciliation.setReconciliationStatus(SubsidiaryReconciliation.RECON_SUBSIDIARY_CONFIRMED);
        reconciliation.setSubsidiaryConfirmBy(confirmBy);
        reconciliation.setSubsidiaryConfirmAt(LocalDateTime.now());

        notificationService.createNotification(
                1L,
                Notification.TYPE_SYSTEM_ALERT,
                "子公司对账已确认",
                String.format("子公司对账记录 %d 已由 %s 确认", reconciliationId, confirmBy),
                reconciliationId,
                "SubsidiaryReconciliation"
        );

        return reconciliationRepository.save(reconciliation);
    }

    @Transactional
    public SubsidiaryReconciliation groupConfirm(Long reconciliationId, String confirmBy) {
        SubsidiaryReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (reconciliation.getReconciliationStatus() != SubsidiaryReconciliation.RECON_SUBSIDIARY_CONFIRMED) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        reconciliation.setReconciliationStatus(SubsidiaryReconciliation.RECON_GROUP_CONFIRMED);
        reconciliation.setGroupConfirmBy(confirmBy);
        reconciliation.setGroupConfirmAt(LocalDateTime.now());

        return reconciliationRepository.save(reconciliation);
    }

    @Transactional(readOnly = true)
    public Page<SubsidiaryReconciliation> getReconciliations(String billMonth, Long orgId, Pageable pageable) {
        if (orgId != null) {
            return reconciliationRepository.findByBillMonthAndSubsidiaryOrgId(billMonth, orgId, pageable);
        }
        return reconciliationRepository.findByBillMonth(billMonth, pageable);
    }

    @Transactional(readOnly = true)
    public SubsidiaryReconciliation getReconciliation(Long id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));
    }

    @Transactional(readOnly = true)
    public List<SubsidiaryReconciliation> getPendingReconciliations(Long orgId) {
        if (orgId != null) {
            return reconciliationRepository.findBySubsidiaryOrgIdAndReconciliationStatus(
                    orgId, SubsidiaryReconciliation.RECON_PENDING);
        }
        return reconciliationRepository.findByReconciliationStatus(
                SubsidiaryReconciliation.RECON_PENDING);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReconciliationSummary(String billMonth) {
        List<SubsidiaryReconciliation> reconciliations = reconciliationRepository.findByBillMonth(billMonth);

        long totalCount = reconciliations.size();
        long pendingCount = reconciliations.stream()
                .filter(r -> r.getReconciliationStatus() == SubsidiaryReconciliation.RECON_PENDING)
                .count();
        long subsidiaryConfirmedCount = reconciliations.stream()
                .filter(r -> r.getReconciliationStatus() == SubsidiaryReconciliation.RECON_SUBSIDIARY_CONFIRMED)
                .count();
        long groupConfirmedCount = reconciliations.stream()
                .filter(r -> r.getReconciliationStatus() == SubsidiaryReconciliation.RECON_GROUP_CONFIRMED)
                .count();

        BigDecimal totalAmount = reconciliations.stream()
                .map(SubsidiaryReconciliation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("billMonth", billMonth);
        result.put("totalOrgs", (int) totalCount);
        result.put("totalAmount", totalAmount.toPlainString());
        result.put("matchedCount", (int) groupConfirmedCount);
        result.put("mismatchedCount", (int) pendingCount);
        result.put("pendingCount", (int) pendingCount + (int) subsidiaryConfirmedCount);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getReconciliationsWithOrgName(String billMonth, Long orgId, Pageable pageable) {
        Page<SubsidiaryReconciliation> page = getReconciliations(billMonth, orgId, pageable);

        // Batch load org names to avoid N+1
        List<Long> orgIds = page.getContent().stream()
                .map(SubsidiaryReconciliation::getSubsidiaryOrgId).distinct().collect(Collectors.toList());
        Map<Long, String> orgNameMap = new HashMap<>();
        if (!orgIds.isEmpty()) {
            orgStructureRepository.findAllById(orgIds).forEach(o -> orgNameMap.put(o.getId(), o.getName()));
        }

        return page.map(r -> {
            String orgName = orgNameMap.getOrDefault(r.getSubsidiaryOrgId(), "Unknown");
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", r.getId());
            result.put("billMonth", r.getBillMonth());
            result.put("orgId", r.getSubsidiaryOrgId());
            result.put("orgName", orgName);
            result.put("totalPhoneCount", 0);
            result.put("totalBillAmount", r.getTotalAmount().toPlainString());
            result.put("invoiceAmount", r.getTotalAmount().toPlainString());
            result.put("diffAmount", "0.00");
            result.put("diffPercentage", 0.0);
            result.put("status", reconStatusName(r.getReconciliationStatus()));
            result.put("subsidiaryConfirm", r.getSubsidiaryConfirmBy() != null ? "CONFIRMED" : "PENDING");
            result.put("subsidiaryConfirmBy", r.getSubsidiaryConfirmBy());
            result.put("subsidiaryConfirmAt", r.getSubsidiaryConfirmAt() != null ? r.getSubsidiaryConfirmAt().format(FORMATTER) : null);
            result.put("groupConfirm", r.getGroupConfirmBy() != null ? "CONFIRMED" : "PENDING");
            result.put("groupConfirmBy", r.getGroupConfirmBy());
            result.put("groupConfirmAt", r.getGroupConfirmAt() != null ? r.getGroupConfirmAt().format(FORMATTER) : null);
            result.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(FORMATTER) : null);
            result.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().format(FORMATTER) : null);
            return result;
        });
    }
    private static String reconStatusName(Integer status) {
        if (status == null) return "UNKNOWN";
        return switch (status) {
            case 0 -> "RECON_PENDING";
            case 1 -> "RECON_SUBSIDIARY_CONFIRMED";
            case 2 -> "RECON_GROUP_CONFIRMED";
            default -> "UNKNOWN";
        };
    }
}
