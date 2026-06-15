package com.phonebiz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.phonebiz.entity.BillAllocation;

/**
 * DTO for BillAllocation list API responses.
 * Avoids exposing Entity directly via REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillAllocationDTO {
    private Long id;
    private Long billRawId;
    private String billMonth;
    private Long phoneId;
    private String phoneNumber;
    private Long snapshotOrgId;
    private String snapshotOrgName;
    private String costCenterCode;
    private BigDecimal chargeAmount;
    private Boolean anomalyFlag;
    private String anomalyReason;
    private BillAllocation.ConfirmStatus adminConfirmOrg;
    private BillAllocation.ConfirmStatus adminConfirmAmount;
    private BillAllocation.FinanceConfirmStatus financeConfirmAnomaly;
    private BillAllocation.FinanceSubmitStatus financeConfirmSubmit;
    private String adminConfirmBy;
    private LocalDateTime adminConfirmAt;
    private String financeConfirmBy;
    private LocalDateTime financeConfirmAt;
    private String financeSubmitBy;
    private LocalDateTime financeSubmitAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BillAllocationDTO from(BillAllocation entity) {
        return BillAllocationDTO.builder()
                .id(entity.getId())
                .billRawId(entity.getBillRawId())
                .billMonth(entity.getBillMonth())
                .phoneId(entity.getPhoneId())
                .phoneNumber(entity.getPhoneNumber())
                .snapshotOrgId(entity.getSnapshotOrgId())
                .snapshotOrgName(entity.getSnapshotOrgName())
                .costCenterCode(entity.getCostCenterCode())
                .chargeAmount(entity.getChargeAmount())
                .anomalyFlag(entity.getAnomalyFlag())
                .anomalyReason(entity.getAnomalyReason())
                .adminConfirmOrg(entity.getAdminConfirmOrg())
                .adminConfirmAmount(entity.getAdminConfirmAmount())
                .financeConfirmAnomaly(entity.getFinanceConfirmAnomaly())
                .financeConfirmSubmit(entity.getFinanceConfirmSubmit())
                .adminConfirmBy(entity.getAdminConfirmBy())
                .adminConfirmAt(entity.getAdminConfirmAt())
                .financeConfirmBy(entity.getFinanceConfirmBy())
                .financeConfirmAt(entity.getFinanceConfirmAt())
                .financeSubmitBy(entity.getFinanceSubmitBy())
                .financeSubmitAt(entity.getFinanceSubmitAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
