package com.phonebiz.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDTO {

    private Long id;
    private String workOrderNo;
    private Integer orderType;
    private Integer status;
    private Integer priority;
    private Long requesterId;
    private String requesterName;
    private Long handlerId;
    private String handlerName;
    private String batchId;
    private String title;
    private String description;
    private LocalDateTime completedAt;
    private String remark;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkOrderItemDTO> items;
}
