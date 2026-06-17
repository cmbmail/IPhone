package com.phonebiz.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fee_allocation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bill_month", "allocation_level", "org_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeAllocation extends BaseEntity {

    public static final int LEVEL_1 = 1; // 一次分摊: 总行→一级分行
    public static final int LEVEL_2 = 2; // 二次分摊: 一级分行→二级分行/部门
    public static final int LEVEL_3 = 3; // 三次分摊: 二级分行→部门

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_REJECTED = 2;

    @Column(name = "bill_month", length = 7, nullable = false)
    private String billMonth;

    @Column(name = "allocation_level", nullable = false)
    private Integer allocationLevel;

    @Column(name = "parent_org_id")
    private Long parentOrgId;

    @Column(name = "parent_org_name", length = 200)
    private String parentOrgName;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "org_name", length = 200, nullable = false)
    private String orgName;

    @Column(name = "org_type")
    private Integer orgType;

    @Column(name = "phone_count")
    @Builder.Default
    private Integer phoneCount = 0;

    @Column(name = "platform_usage_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformUsageFee = BigDecimal.ZERO;

    @Column(name = "number_monthly_rent", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal numberMonthlyRent = BigDecimal.ZERO;

    @Column(name = "domestic_charge", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal domesticCharge = BigDecimal.ZERO;

    @Column(name = "international_charge", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal internationalCharge = BigDecimal.ZERO;

    @Column(name = "recording_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal recordingFee = BigDecimal.ZERO;

    @Column(name = "ringtone_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ringtoneFee = BigDecimal.ZERO;

    @Column(name = "flash_sms_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal flashSmsFee = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = STATUS_PENDING;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
