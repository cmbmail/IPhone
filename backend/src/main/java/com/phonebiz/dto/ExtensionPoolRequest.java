package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExtensionPoolRequest {

    @NotNull(message = "Organization ID is required")
    private Long orgId;

    @NotBlank(message = "Start number is required")
    private String startNumber;

    @NotBlank(message = "End number is required")
    private String endNumber;
}
