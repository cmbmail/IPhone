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
public class BindPhoneRequest {
    @NotNull(message = "分机号不能为空")
    private String extensionNumber;

    private String remark;
}
