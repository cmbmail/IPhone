package com.phonebiz.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "phone_device", uniqueConstraints = {
    @UniqueConstraint(columnNames = "mac_address")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mac_address", nullable = false, length = 12)
    private String macAddress;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "assigned_to", length = 20)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhoneDeviceStatus status;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "version", nullable = false)
    @Version
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 0;
        }
        if (status == null) {
            status = PhoneDeviceStatus.stock;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PhoneDeviceStatus {
        stock, active, inactive, repairing, retired
    }
}

