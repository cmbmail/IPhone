package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class CreateAreaCodeMappingRequest {

    @NotBlank(message = "Area code is required")
    @Pattern(regexp = "^0\\d{2,3}$", message = "Invalid area code format (e.g., 010, 021)")
    private String areaCode;

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    private Integer priority = 1;
}
