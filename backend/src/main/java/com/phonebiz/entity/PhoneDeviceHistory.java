package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "phone_device_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDeviceHistory extends BaseEntity {

    @Column(name = "phone_device_id", nullable = false)
    private Long phoneDeviceId;

    @Column(name = "mac_address", nullable = false, length = 20)
    private String macAddress;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "from_status")
    private Integer fromStatus;

    @Column(name = "to_status")
    private Integer toStatus;

    @Column(name = "from_assigned", length = 20)
    private String fromAssigned;

    @Column(name = "to_assigned", length = 20)
    private String toAssigned;

    @Column(name = "operator", nullable = false, length = 100)
    private String operator;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (operatedAt == null) operatedAt = LocalDateTime.now();
    }
}
