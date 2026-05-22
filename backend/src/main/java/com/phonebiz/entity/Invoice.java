package com.phonebiz.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoice")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    public static final int INV_PENDING = 0;
    public static final int INV_DISTRIBUTED = 1;
    public static final int INV_READ = 2;
    public static final int INV_CONFIRMED = 3;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version

    @Column(name = "version")
    private Long version = 0L;

    @Column(name = "invoice_no", length = 100, nullable = false, unique = true)
    private String invoiceNo;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "source_org_id", nullable = false)
    private Long sourceOrgId;

    @Column(name = "source_org_name", length = 100, nullable = false)
    private String sourceOrgName;

    @Column(name = "recipient_org_id", nullable = false)
    private Long recipientOrgId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Integer status = Invoice.INV_PENDING;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "ocr_confidence", precision = 5, scale = 4)
    private BigDecimal ocrConfidence;

    @Column(name = "distribute_at")
    private LocalDateTime distributeAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}