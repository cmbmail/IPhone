package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoice_distribution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDistribution {

    public static final int DIST_FAILED = 0;
    public static final int DIST_SUCCESS = 1;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "recipient_user", length = 50, nullable = false)
    private String recipientUser;
    @Column(name = "distribution_status", nullable = false, length = 20)
    @Builder.Default
    private DistributionStatus distributionStatus = DistributionStatus.success;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt;

    @PrePersist
    protected void onCreate() {
        notifiedAt = LocalDateTime.now();
    }

    public enum DistributionStatus {
        success,
        failed
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}