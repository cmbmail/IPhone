package com.phonebiz.dto;

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
public class UpdateWorkOrderRequest {

    private String status;

    private Long handlerId;

    private String remark;

    private String description;
}

