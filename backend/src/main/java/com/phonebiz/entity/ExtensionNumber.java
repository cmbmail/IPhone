package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "extension_number")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionNumber {

    public static final int EXT_AVAILABLE = 0;
    public static final int EXT_ALLOCATED = 1;
    public static final int EXT_IDLE = 2;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "extension_number", nullable = false, length = 20, unique = true)
    private String extensionNumber;

    @Column(name = "pool_id", nullable = false)
    private Long poolId;
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Integer status = ExtensionNumber.EXT_AVAILABLE;

    @Column(name = "employee_name", length = 100)
    private String employeeName;

    @Column(name = "dept_name", length = 100)
    private String deptName;
    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "dept_org_id")
    private Long deptOrgId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "work_order_id")
    private Long workOrderId;

    @Column(name = "created_by", length = 50, nullable = false)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50, nullable = false)
    @Builder.Default
    private String updatedBy = "system";

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = java.time.LocalDateTime.now(); updatedAt = java.time.LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = java.time.LocalDateTime.now(); }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
