package com.phonebiz.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_operation")
public class DeviceOperation extends BaseEntity {

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public enum OperationType {
        REBOOT,
        CONFIG_SYNC,
        FIRMWARE_UPGRADE,
        FACTORY_RESET,
        REGISTER
    }

    public enum OperationStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
