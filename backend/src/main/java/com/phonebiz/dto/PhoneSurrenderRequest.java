package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhoneSurrenderRequest {

    @NotNull(message = "Phone ID is required")
    private Long phoneId;

    @NotNull(message = "Surrender type is required")
    private String surrenderType;

    private String workOrderNo;

    private String remark;
}
