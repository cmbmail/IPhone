package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PhoneAllocationRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    @NotNull(message = "User ID is required")
    private String employeeNo;

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    private String extensionNumber;

    private String workOrderNo;

    private String remark;
}
