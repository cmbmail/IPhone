package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "invoice_distribution")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDistribution extends BaseEntity {

    public static final int DIST_FAILED = 0;
    public static final int DIST_SUCCESS = 1;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "recipient_user", length = 50, nullable = false)
    private String recipientUser;

    @Column(name = "distribution_status", nullable = false)
    @Builder.Default
    private Integer distributionStatus = DIST_SUCCESS;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt;

    @PrePersist
    protected void onCreate() {
        if (notifiedAt == null) notifiedAt = LocalDateTime.now();
    }
}
