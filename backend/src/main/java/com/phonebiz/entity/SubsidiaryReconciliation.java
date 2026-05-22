package com.phonebiz.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subsidiary_reconciliation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsidiaryReconciliation {

    public static final int RECON_PENDING = 0;
    public static final int RECON_SUBSIDIARY_CONFIRMED = 1;
    public static final int RECON_GROUP_CONFIRMED = 2;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "subsidiary_org_id", nullable = false)
    private Long subsidiaryOrgId;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "invoice_count", nullable = false)
    @Builder.Default
    private Integer invoiceCount = 0;
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.pending;

    @Column(name = "subsidiary_confirm_by", length = 50)
    private String subsidiaryConfirmBy;

    @Column(name = "subsidiary_confirm_at")
    private LocalDateTime subsidiaryConfirmAt;

    @Column(name = "group_confirm_by", length = 50)
    private String groupConfirmBy;

    @Column(name = "group_confirm_at")
    private LocalDateTime groupConfirmAt;

    @Column(name = "remark", length = 500)
    private String remark;

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

    public enum ReconciliationStatus {
        pending,
        confirmed_by_subsidiary,
        confirmed_by_group
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
