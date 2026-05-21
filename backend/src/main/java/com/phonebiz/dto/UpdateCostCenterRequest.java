package com.phonebiz.dto;

import lombok.Data;

@Data
public class UpdateCostCenterRequest {
    private String costCenterName;
    private String costCenterCode;
    private String status;
}
