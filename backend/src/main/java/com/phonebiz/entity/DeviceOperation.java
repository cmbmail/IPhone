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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_operation")
public class DeviceOperation extends BaseEntity {

    public static final int OP_PENDING = 0;
    public static final int OP_PROCESSING = 1;
    public static final int OP_COMPLETED = 2;
    public static final int OP_FAILED = 3;


    public static final int OP_REBOOT = 1;
    public static final int OP_CONFIG_SYNC = 2;
    public static final int OP_FIRMWARE_UPGRADE = 3;
    public static final int OP_FACTORY_RESET = 4;
    public static final int OP_REGISTER = 5;


    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;
    @Column(name = "operation_type", nullable = false, length = 20)
    private Integer operationType;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
