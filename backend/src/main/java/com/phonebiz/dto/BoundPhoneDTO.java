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
public class BoundPhoneDTO {
    private Long phoneId;
    private String phoneNumber;
    private String extensionNumber;
    private String status;
    private String userId;
    private String userName;
    private Long orgId;
    private String orgName;
    private Integer lineOrder;
    private LocalDateTime createdAt;
}

