package com.phonebiz.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bill_allocation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "bill_raw_id", nullable = false)
    private Long billRawId;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "snapshot_org_id")
    private Long snapshotOrgId;

    @Column(name = "snapshot_org_name", length = 100)
    private String snapshotOrgName;

    @Column(name = "cost_center_code", length = 50)
    private String costCenterCode;

    @Column(name = "charge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeAmount;

    @Column(name = "anomaly_flag", nullable = false)
    @Builder.Default
    private Boolean anomalyFlag = false;

    @Column(name = "anomaly_reason", length = 500)
    private String anomalyReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_confirm_org", nullable = false, length = 20)
    @Builder.Default
    private ConfirmStatus adminConfirmOrg = ConfirmStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_confirm_amount", nullable = false, length = 20)
    @Builder.Default
    private ConfirmStatus adminConfirmAmount = ConfirmStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "finance_confirm_anomaly", nullable = false, length = 20)
    @Builder.Default
    private FinanceConfirmStatus financeConfirmAnomaly = FinanceConfirmStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "finance_confirm_submit", nullable = false, length = 20)
    @Builder.Default
    private FinanceSubmitStatus financeConfirmSubmit = FinanceSubmitStatus.pending;

    @Column(name = "admin_confirm_by", length = 50)
    private String adminConfirmBy;

    @Column(name = "admin_confirm_at")
    private LocalDateTime adminConfirmAt;

    @Column(name = "finance_confirm_by", length = 50)
    private String financeConfirmBy;

    @Column(name = "finance_confirm_at")
    private LocalDateTime financeConfirmAt;

    @Column(name = "finance_submit_by", length = 50)
    private String financeSubmitBy;

    @Column(name = "finance_submit_at")
    private LocalDateTime financeSubmitAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ConfirmStatus {
        pending,
        correct,
        wrong
    }

    public enum FinanceConfirmStatus {
        pending,
        confirmed,
        rejected
    }

    public enum FinanceSubmitStatus {
        pending,
        submitted
    }
}
