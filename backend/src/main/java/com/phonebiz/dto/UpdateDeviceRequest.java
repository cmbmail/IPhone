package com.phonebiz.dto;

import lombok.Data;

@Data
public class UpdateDeviceRequest {

    private String deviceName;

    private String model;

    private String macAddress;

    private String ipAddress;

    private String phoneNumber;

    private String extensionNumber;

    private String status;

    private String firmwareVersion;

    private String remark;
}
