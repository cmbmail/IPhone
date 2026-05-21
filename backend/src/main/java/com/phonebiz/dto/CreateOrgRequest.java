package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateOrgRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    private Long parentId;

    private String type;

    private String remark;

    private String branchName;

    private String orgCode;

    private String costCenter;
}
