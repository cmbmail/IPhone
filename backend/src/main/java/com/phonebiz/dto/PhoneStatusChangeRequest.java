package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneStatusChangeRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    @NotNull(message = "New status is required")
    @Pattern(regexp = "^(idle|active|stopped|cancelled|reserved|disabled)$", message = "Invalid phone status")
    private String newStatus;

    private String workOrderNo;

    private String remark;
}
