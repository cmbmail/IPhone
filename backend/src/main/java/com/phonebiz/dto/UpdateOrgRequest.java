package com.phonebiz.dto;

import lombok.Data;

@Data
public class UpdateOrgRequest {

    private String name;

    private Integer type;

    private Integer status;

    private Integer sortOrder;

    private String branchName;

    private String orgCode;

    private String costCenterCode;
}
