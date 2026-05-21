package com.phonebiz.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhoneDeviceRequest {
    @NotNull(message = "MAC地址不能为空")
    private String macAddress;

    @Size(max = 100, message = "型号最长100字符")
    private String model;

    @Size(max = 100, message = "品牌最长100字符")
    private String brand;

    private LocalDate purchaseDate;

    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;
}

