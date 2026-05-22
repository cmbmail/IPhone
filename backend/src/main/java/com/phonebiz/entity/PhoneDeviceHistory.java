package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "phone_device_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDeviceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "mac_address", nullable = false, length = 12)
    private String macAddress;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20)
    private String toStatus;

    @Column(name = "from_assigned", length = 20)
    private String fromAssigned;

    @Column(name = "to_assigned", length = 20)
    private String toAssigned;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "remark", length = 500)
    private String remark;

    @PrePersist
    protected void onCreate() {
        if (operatedAt == null) {
            operatedAt = LocalDateTime.now();
        }
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

