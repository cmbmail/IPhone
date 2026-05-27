package com.phonebiz.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "org_structure")
public class OrgStructure extends BaseEntity {

    public static final int ORG_INACTIVE = 0;
    public static final int ORG_ACTIVE = 1;


    public static final int ORG_GROUP = 1;       // 集团
    public static final int ORG_BRANCH = 2;      // 分行（一级分行、二级分行、总行）
    public static final int ORG_DEPT = 3;        // 部门（一级部门、二级部门）
    public static final int ORG_COMP_SUB = 4;    // 综合支行
    public static final int ORG_RETL_SUB = 5;    // 零专支行（综合支行下级）

    /** Check if this org is a branch type (can receive phone numbers from system pool) */
    public boolean isBranch() {
        return this.type == ORG_BRANCH;
    }

    /** Check if this org is a department type */
    public boolean isDepartment() {
        return this.type == ORG_DEPT;
    }

    /** Check if this org is a sub-branch type (综合支行 or 零专支行) */
    public boolean isSubBranch() {
        return this.type == ORG_COMP_SUB || this.type == ORG_RETL_SUB;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private Integer type = OrgStructure.ORG_DEPT;

    @Column(nullable = false)
    private Integer level = 0;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 500)
    private String remark;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "org_code", length = 50)
    private String orgCode;

    @Column(name = "cost_center_code", length = 50)
    private String costCenterCode;
    @Column(nullable = false)
    private Integer status = OrgStructure.ORG_ACTIVE;

    @OneToMany(mappedBy = "parentId", fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<OrgStructure> children = new ArrayList<>();

    public void calculateLevelAndPath() {
        if (this.parentId == null) {
            this.level = 0;
            this.path = "/" + this.id;
        } else {
            this.path = this.path + "/" + this.id;
        }
    }

    /**
     * Auto-calculate level from path depth.
     * Level = number of segments in path - 1 (e.g., /1/2/3 → level=2)
     */
    public void recalculateLevel() {
        if (this.path != null && !this.path.isEmpty()) {
            int slashCount = this.path.length() - this.path.replace("/", "").length();
            this.level = Math.max(0, slashCount - 1);
        }
    }

    /**
     * Validate that this org's type is compatible with its parent.
     * Rules:
     *   集团(1) → 分行(2)
     *   分行(2) → 分行(2) / 部门(3) / 综合支行(4)
     *   部门(3) → 部门(3)
     *   综合支行(4) → 零专支行(5)
     *   零专支行(5) → no children
     */
    public void validateChildType(int childType) {
        boolean valid = switch (this.type) {
            case ORG_GROUP -> childType == ORG_BRANCH;
            case ORG_BRANCH -> childType == ORG_BRANCH || childType == ORG_DEPT || childType == ORG_COMP_SUB;
            case ORG_DEPT -> childType == ORG_DEPT;
            case ORG_COMP_SUB -> childType == ORG_RETL_SUB;
            case ORG_RETL_SUB -> false;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                "类型不兼容: " + getTypeName(this.type) + " 下不能创建 " + getTypeName(childType));
        }
    }

    public static String getTypeName(int type) {
        return switch (type) {
            case ORG_GROUP -> "集团";
            case ORG_BRANCH -> "分行";
            case ORG_DEPT -> "部门";
            case ORG_COMP_SUB -> "综合支行";
            case ORG_RETL_SUB -> "零专支行";
            default -> "未知";
        };
    }

    public void validateCycle(Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (newParentId.equals(this.id)) {
            throw new BusinessException(ErrorCode.ORG_003);
        }
    }
}
