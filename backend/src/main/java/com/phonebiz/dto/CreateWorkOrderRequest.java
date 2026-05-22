package com.phonebiz.dto;

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
public class CreateWorkOrderRequest {

    @NotNull(message = "工单类型不能为空")
    private Integer type;

    @NotBlank(message = "工单标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    private Long handlerId;

    private List<WorkOrderItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderItemRequest {
        private Integer itemType;
        private Long targetId;
        private String action;
        private String fromValue;
        private String toValue;
    }
}
