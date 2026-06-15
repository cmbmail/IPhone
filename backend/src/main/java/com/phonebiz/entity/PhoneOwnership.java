package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@Where(clause = "deleted_at IS NULL")
@Table(name = "phone_ownership")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "branch_org_id")
    private Long branchOrgId;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "dept_org_id")
    private Long deptOrgId;

    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Column(name = "remark", length = 500)
    private String remark;

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
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
