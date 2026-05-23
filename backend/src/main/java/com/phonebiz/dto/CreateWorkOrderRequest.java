package com.phonebiz.dto;

import java.util.List;

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

    // Frontend form fields
    private String employeeName;
    private String employeeNo;
    private String extensionNumber;
    private String macAddresses;
    private String branchName;
    private String deptName;
    private String remark;

    // Driven service fields (optional - used by programmatic work order creation)
    private String title;
    private String description;
    private Integer priority;
    private List<WorkOrderItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderItemRequest {
        private Integer itemType;
        private Long targetRefId;
        private String action;
        private String fromValue;
        private String toValue;
    }
}
