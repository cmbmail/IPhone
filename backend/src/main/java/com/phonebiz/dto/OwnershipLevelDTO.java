package com.phonebiz.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipLevelDTO {

    private Long orgId;
    private String orgName;
    private Integer orgType;
    private String orgTypeName;
    private Long parentOrgId;
    private String parentOrgName;
    private Integer phoneCount;
    private Integer allocatedCount;

    /** Level summary response */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelSummaryResponse {
        private Integer level;
        private String levelName;
        private String levelDescription;
        private Integer totalOrgs;
        private Integer totalPhones;
        private Integer totalAllocated;
        private List<OwnershipLevelDTO> items;
    }
}
