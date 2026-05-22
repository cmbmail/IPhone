package com.phonebiz.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillAllocationSummaryDTO {
    private String branchName;
    private BigDecimal platformUsageFee;
    private BigDecimal numberMonthlyRent;
    private Integer outboundDuration;
    private Integer transferOutboundDuration;
    private BigDecimal domesticCharge;
    private BigDecimal internationalCharge;
    private BigDecimal feeSubtotal;
    private BigDecimal recordingFee;
    private BigDecimal ringtoneFee;
    private BigDecimal flashSmsFee;
    private BigDecimal totalAmount;
}
