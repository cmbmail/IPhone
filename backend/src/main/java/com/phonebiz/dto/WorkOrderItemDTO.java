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
public class WorkOrderItemDTO {

    private Long id;
    private Long workOrderId;
    private Integer itemType;
    private Long targetRefId;
    private String action;
    private String fromValue;
    private String toValue;
    private Integer status;
    private LocalDateTime executedAt;
    private String errorMessage;
    private String remark;
    private String description;
}
