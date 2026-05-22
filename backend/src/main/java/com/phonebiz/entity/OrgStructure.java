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


    public static final int ORG_GROUP = 1;
    public static final int ORG_SUBSIDIARY = 2;
    public static final int ORG_DEPT = 3;


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

    @Column(name = "cost_center", length = 50)
    private String costCenter;
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

    public void validateCycle(Long newParentId) {
        if (newParentId == null) {
            return;
        }
        if (newParentId.equals(this.id)) {
            throw new BusinessException(ErrorCode.ORG_003);
        }
    }
}
