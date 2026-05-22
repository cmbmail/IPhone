package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DeleteBillRequest {
    @NotBlank(message = "billMonth is required")
    @Pattern(regexp = "^\\d{6}$", message = "billMonth must be yyyyMM format")
    private String billMonth;

    @NotBlank(message = "chargeType is required")
    private Integer chargeType;

    @NotBlank(message = "password is required")
    private String password;
}
