package com.phonebiz.dto;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPhoneDeviceRequest {
    @NotNull(message = "工号不能为空")
    private String employeeNo;

    private String remark;
}
