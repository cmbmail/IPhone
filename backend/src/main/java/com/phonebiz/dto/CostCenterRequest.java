package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCostCenterRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    @NotBlank(message = "Cost center name is required")
    private String costCenterName;

    @NotBlank(message = "Cost center code is required")
    private String costCenterCode;

    private String status = "active";
}
