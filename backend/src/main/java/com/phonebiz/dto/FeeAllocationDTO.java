package com.phonebiz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.phonebiz.entity.FeeAllocation;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeAllocationDTO {

    private Long id;
    private String billMonth;
    private Integer allocationLevel;
    private Long parentOrgId;
    private String parentOrgName;
    private Long orgId;
    private String orgName;
    private Integer orgType;
    private String orgTypeName;
    private Integer phoneCount;
    private BigDecimal platformUsageFee;
    private BigDecimal numberMonthlyRent;
    private BigDecimal domesticCharge;
    private BigDecimal internationalCharge;
    private BigDecimal recordingFee;
    private BigDecimal ringtoneFee;
    private BigDecimal flashSmsFee;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
    private Integer status;
    private String statusName;
    private String confirmedBy;
    private LocalDateTime confirmedAt;

    public static FeeAllocationDTO from(FeeAllocation fa) {
        return FeeAllocationDTO.builder()
                .id(fa.getId())
                .billMonth(fa.getBillMonth())
                .allocationLevel(fa.getAllocationLevel())
                .parentOrgId(fa.getParentOrgId())
                .parentOrgName(fa.getParentOrgName())
                .orgId(fa.getOrgId())
                .orgName(fa.getOrgName())
                .orgType(fa.getOrgType())
                .orgTypeName(getOrgTypeName(fa.getOrgType()))
                .phoneCount(fa.getPhoneCount())
                .platformUsageFee(fa.getPlatformUsageFee())
                .numberMonthlyRent(fa.getNumberMonthlyRent())
                .domesticCharge(fa.getDomesticCharge())
                .internationalCharge(fa.getInternationalCharge())
                .recordingFee(fa.getRecordingFee())
                .ringtoneFee(fa.getRingtoneFee())
                .flashSmsFee(fa.getFlashSmsFee())
                .totalAmount(fa.getTotalAmount())
                .percentage(fa.getPercentage())
                .status(fa.getStatus())
                .statusName(getStatusName(fa.getStatus()))
                .confirmedBy(fa.getConfirmedBy())
                .confirmedAt(fa.getConfirmedAt())
                .build();
    }

    private static String getOrgTypeName(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case 1 -> "集团";
            case 2 -> "分行";
            case 3 -> "部门";
            case 4 -> "综合支行";
            case 5 -> "零专支行";
            default -> "未知";
        };
    }

    private static String getStatusName(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "待确认";
            case 1 -> "已确认";
            case 2 -> "已驳回";
            default -> "未知";
        };
    }

    /** Summary response for a single allocation level */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelResponse {
        private String billMonth;
        private Integer allocationLevel;
        private String levelName;
        private String levelDescription;
        private Integer totalCount;
        private Integer totalPhones;
        private BigDecimal totalAmount;
        private BigDecimal totalPlatformUsageFee;
        private BigDecimal totalNumberMonthlyRent;
        private BigDecimal totalDomesticCharge;
        private BigDecimal totalInternationalCharge;
        private BigDecimal totalRecordingFee;
        private BigDecimal totalRingtoneFee;
        private BigDecimal totalFlashSmsFee;
        private Boolean calculated;
        private List<FeeAllocationDTO> items;
    }
}
