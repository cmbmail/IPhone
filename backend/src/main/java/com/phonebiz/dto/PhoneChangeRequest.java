package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PhoneChangeRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    private String employeeNo;

    private Long orgId;

    private String phoneNumber;

    private String extensionNumber;

    private String workOrderNo;

    private String remark;
}
