package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class CreatePhoneRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String userId;

    @Pattern(regexp = "^\\d{3,8}$", message = "Invalid extension number format")
    private String extensionNumber;

    private String extensionType;

    private Long orgId;

    private String remark;
}
