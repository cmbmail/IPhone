package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "phone_snapshot", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"snapshot_month", "phone_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneSnapshot extends BaseEntity {

    // Allocation status constants
    public static final int ALLOC_NOT_ALLOCATED = 0;
    public static final int ALLOC_ALLOCATED = 1;
    public static final int ALLOC_ANOMALY = 2;

    @Column(name = "snapshot_month", length = 7, nullable = false)
    private String snapshotMonth;

    @Column(name = "bill_month", length = 7)
    private String billMonth;

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "extension_number", length = 50)
    private String extensionNumber;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 0;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "branch_org_id")
    private Long branchOrgId;

    @Column(name = "org_name", length = 200)
    private String orgName;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "cost_center_code", length = 50)
    private String costCenterCode;

    @Column(name = "employee_no", length = 50)
    private String employeeNo;

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "is_surrendered")
    @Builder.Default
    private Boolean isSurrendered = false;

    @Column(name = "is_allocatable")
    @Builder.Default
    private Boolean isAllocatable = false;

    @Column(name = "allocation_status", nullable = false)
    @Builder.Default
    private Integer allocationStatus = ALLOC_NOT_ALLOCATED;
}
