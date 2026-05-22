package com.phonebiz.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDeviceDTO {
    private Long id;
    private String macAddress;
    private String model;
    private String brand;
    private LocalDate purchaseDate;
    private Long orgId;
    private String orgName;
    private String assignedTo;
    private String assignedEmployeeName;
    private Integer status;
    private String remark;
    private Integer boundPhoneCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

