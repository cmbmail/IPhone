package com.phonebiz.dto;

import lombok.Data;

@Data
public class UpdateOrgRequest {

    private String name;

    private String type;

    private String status;

    private Integer sortOrder;

    private String branchName;

    private String orgCode;

    private String costCenter;
}
