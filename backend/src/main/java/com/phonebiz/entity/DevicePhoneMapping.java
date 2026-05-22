package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "device_phone_mapping", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"device_id", "phone_id"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePhoneMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_device_id", nullable = false)
    private Long phoneDeviceId;

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lineOrder == null) {
            lineOrder = 1;
        }
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

