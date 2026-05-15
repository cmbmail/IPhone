package com.phonebiz.entity;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "org_structure")
public class OrgStructure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgType type = OrgType.dept;

    @Column(nullable = false)
    private Integer level = 0;

    @Column(nullable = false, length = 500)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgStatus status = OrgStatus.active;

    @OneToMany(mappedBy = "parentId", fetch = FetchType.LAZY)
    private List<OrgStructure> children = new ArrayList<>();

    public enum OrgType {
        group, subsidiary, dept
    }

    public enum OrgStatus {
        active, inactive
    }

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
        OrgStructure parent = findParent(newParentId);
        if (parent != null && parent.getPath().contains("/" + this.id + "/")) {
            throw new BusinessException(ErrorCode.ORG_003);
        }
    }

    private OrgStructure findParent(Long parentId) {
        return null;
    }
}
