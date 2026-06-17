package com.phonebiz.dto;

import lombok.AllArgsConstructor;
import com.phonebiz.entity.PhoneOwnership;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PhoneOwnership view object with allocation-level columns.
 * Level1 = 一级分行
 * Level2 = 二级分行 / 一级分行部门 / 综合支行
 * Level3 = 部门 / 支行 / 零专支行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOwnershipVO {
    private Long id;
    private String phoneNumber;
    private Long branchOrgId;
    private String branchName;
    private Long deptOrgId;
    private String deptName;
    private String remark;

    // Allocation level columns
    private String level1BranchName;  // 一级分行
    private Long level1BranchOrgId;
    private String level2OrgName;     // 二级分行/一级部门/综合支行
    private Long level2OrgId;
    private String level3OrgName;     // 部门/支行/零专支行
    private Long level3OrgId;

    public static PhoneOwnershipVO from(PhoneOwnership po) {
        return PhoneOwnershipVO.builder()
                .id(po.getId())
                .phoneNumber(po.getPhoneNumber())
                .branchOrgId(po.getBranchOrgId())
                .branchName(po.getBranchName())
                .deptOrgId(po.getDeptOrgId())
                .deptName(po.getDeptName())
                .remark(po.getRemark())
                .build();
    }
}
