package com.phonebiz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class BranchAllocateRequest {

    @NotEmpty(message = "Phone IDs are required")
    private java.util.List<Long> phoneIds;

    @NotNull(message = "Branch org ID is required")
    private Long branchOrgId;

    private String remark;
}
