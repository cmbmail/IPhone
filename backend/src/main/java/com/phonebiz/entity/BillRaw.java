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
@Table(name = "bill_raw")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "file_name", length = 200, nullable = false)
    private String fileName;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "charge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeAmount;

    @Column(name = "charge_type", length = 50)
    private String chargeType;

    @Column(name = "billing_start_date")
    private LocalDate billingStartDate;

    @Column(name = "billing_end_date")
    private LocalDate billingEndDate;

    @Column(name = "raw_data", columnDefinition = "JSON")
    private String rawData;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 20)
    @Builder.Default
    private ImportStatus importStatus = ImportStatus.pending;

    @Column(name = "import_error_msg", length = 500)
    private String importErrorMsg;

    @Column(name = "imported_by", length = 50, nullable = false)
    private String importedBy;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;


    @Column(name = "user_id", length = 20)
    private String userId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "extension_number", length = 20)
    private String extensionNumber;

    @Column(name = "allocation_time")
    private LocalDate allocationTime;

    @Column(name = "activation_time")
    private LocalDate activationTime;

    @Column(name = "deactivation_time")
    private LocalDate deactivationTime;

    @Column(name = "platform_usage_fee", precision = 12, scale = 2)
    private BigDecimal platformUsageFee;

    @Column(name = "number_monthly_rent", precision = 12, scale = 2)
    private BigDecimal numberMonthlyRent;

    @Column(name = "outbound_duration")
    private Integer outboundDuration;

    @Column(name = "transfer_outbound_duration")
    private Integer transferOutboundDuration;

    @Column(name = "domestic_charge", precision = 12, scale = 2)
    private BigDecimal domesticCharge;

    @Column(name = "international_duration")
    private Integer internationalDuration;

    @Column(name = "international_charge", precision = 12, scale = 2)
    private BigDecimal internationalCharge;

    @Column(name = "days")
    private Integer days;

    @Column(name = "remark", length = 500)
    private String remark;
    @Column(name = "sub_number", length = 20)
    private String subNumber;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "send_count")
    private Integer sendCount;

    @Column(name = "closing_time", length = 50)
    private String closingTime;


    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }

    public enum ImportStatus {
        pending,
        processed,
        error
    }
}
