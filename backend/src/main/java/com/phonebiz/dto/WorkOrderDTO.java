package com.phonebiz.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.phonebiz.entity.WorkOrder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDTO {

    private Long id;
    private String workOrderNo;
    private String type;
    private String status;
    private String priority;
    private Long requesterId;
    private String requesterName;
    private Long handlerId;
    private String handlerName;
    private String batchId;
    private String title;
    private String description;
    private LocalDateTime completedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkOrderItemDTO> items;
}

