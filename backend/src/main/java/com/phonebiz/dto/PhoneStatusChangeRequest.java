package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PhoneStatusChangeRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    @NotNull(message = "New status is required")
    private String newStatus;

    private String workOrderNo;

    private String remark;
}
