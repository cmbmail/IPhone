package com.phonebiz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class DeptAllocateRequest {

    @NotEmpty(message = "Phone IDs are required")
    private java.util.List<Long> phoneIds;

    @NotNull(message = "Department org ID is required")
    private Long deptOrgId;

    private String remark;
}
