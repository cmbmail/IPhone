package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PhoneReclaimRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    private String reason;

    private String workOrderNo;

    private String remark;
}
