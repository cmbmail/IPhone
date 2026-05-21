package com.phonebiz.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDeviceHistoryDTO {
    private Long id;
    private Long deviceId;
    private String macAddress;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String fromAssigned;
    private String toAssigned;
    private String operator;
    private LocalDateTime operatedAt;
    private String remark;
}

