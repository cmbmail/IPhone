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
public class PhoneSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_month", length = 7, nullable = false)
    private String snapshotMonth;

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "org_name", length = 200)
    private String orgName;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}