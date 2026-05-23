package com.phonebiz.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "bill_raw")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillRaw extends BaseEntity {

    public static final int IMPORT_PENDING = 0;
    public static final int IMPORT_PROCESSED = 1;
    public static final int IMPORT_ERROR = 2;

    public static final int CHARGE_MONTHLY_RENT = 1;
    public static final int CHARGE_CALL = 2;
    public static final int CHARGE_SMS = 3;
    public static final int CHARGE_DATA = 4;
    public static final int CHARGE_OTHER = 5;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "file_name", length = 200, nullable = false)
    private String fileName;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "charge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargeAmount;

    public static final int CHARGE_TYPE_PHONE = 0;
    public static final int CHARGE_TYPE_RECORDING = 1;
    public static final int CHARGE_TYPE_RINGTONE = 2;
    public static final int CHARGE_TYPE_FLASH_SMS = 3;

    @Column(name = "charge_type")
    private Integer chargeType;

    @Column(name = "billing_start_date")
    private LocalDate billingStartDate;

    @Column(name = "billing_end_date")
    private LocalDate billingEndDate;

    @Column(name = "raw_data", columnDefinition = "JSON")
    private String rawData;

    @Column(name = "import_status", nullable = false)
    @Builder.Default
    private Integer importStatus = IMPORT_PENDING;

    @Column(name = "import_error_msg", length = 500)
    private String importErrorMsg;

    @Column(name = "imported_by", length = 50, nullable = false)
    private String importedBy;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "employee_no", length = 20)
    private String employeeNo;

    @Column(name = "dept_name", length = 100)
    private String deptName;

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
        if (importedAt == null) importedAt = LocalDateTime.now();
    }
}
