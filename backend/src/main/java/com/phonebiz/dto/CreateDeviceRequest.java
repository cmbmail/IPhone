package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDeviceRequest {

    @NotBlank(message = "Device ID cannot be blank")
    private String deviceId;

    private String deviceName;

    @NotNull(message = "Device type cannot be null")
    private Integer deviceType;

    private String model;

    private String macAddress;

    private String ipAddress;

    private String phoneNumber;

    private String extensionNumber;

    @NotNull(message = "Status cannot be null")
    private Integer status;

    private String firmwareVersion;

    private String remark;
}
