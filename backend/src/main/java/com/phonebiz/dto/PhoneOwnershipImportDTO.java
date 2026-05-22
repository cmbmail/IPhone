package com.phonebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOwnershipImportDTO {
    private String phoneNumber;
    private String branchName;
    private String deptName;
    private String remark;
    // Existing data for comparison
    private String existingBranchName;
    private String existingDeptName;
    private String existingRemark;
    private boolean isNew;
    private boolean hasDiff;
}
