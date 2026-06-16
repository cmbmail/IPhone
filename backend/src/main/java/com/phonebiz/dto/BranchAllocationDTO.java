package com.phonebiz.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Branch-level allocation summary DTO.
 * Aggregates bill_allocation data by branch (from phone_snapshot).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchAllocationDTO {

    private Long branchOrgId;
    private String branchName;
    private Integer phoneCount;
    private BigDecimal totalChargeAmount;

    /** Fee breakdown */
    private BigDecimal platformUsageFee;
    private BigDecimal numberMonthlyRent;
    private BigDecimal domesticCharge;
    private BigDecimal internationalCharge;
    private BigDecimal otherCharge;

    /** Allocation status counts */
    private Integer allocatedCount;
    private Integer anomalyCount;
    private Integer unallocatedCount;

    /** Fee percentage of total */
    private BigDecimal chargePercentage;

    /**
     * Full response with total summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchAllocationResponse {
        private String billMonth;
        private String snapshotMonth;
        private Integer totalBranches;
        private Integer totalPhones;
        private BigDecimal totalAmount;
        private List<BranchAllocationDTO> branches;
    }
}
