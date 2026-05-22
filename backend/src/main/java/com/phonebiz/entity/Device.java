package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

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
@Table(name = "device")
public class Device extends BaseEntity {

    public static final int DEV_ONLINE = 1;
    public static final int DEV_OFFLINE = 2;
    public static final int DEV_UNREGISTERED = 3;
    public static final int DEV_DISABLED = 4;


    public static final int DEV_IP_PHONE = 1;
    public static final int DEV_SOFT_PHONE = 2;
    public static final int DEV_ATA = 3;
    public static final int DEV_GATEWAY = 4;


    @NotBlank(message = "Device ID cannot be blank")
    @Column(name = "device_id", unique = true, nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;
    @Column(name = "device_type", nullable = false, length = 20)
    private Integer deviceType;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "mac_address", unique = true, length = 20)
    private String macAddress;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "extension_number", length = 20)
    private String extensionNumber;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "last_checkin_time")
    private java.time.LocalDateTime lastCheckinTime;

    @Column(name = "remark", length = 500)
    private String remark;
}
