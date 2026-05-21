package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import com.phonebiz.entity.OrgStructure;

@Data
public class OrgDTO {

    @NotBlank(message = "Organization name is required")
    private String name;

    private Long parentId;

    private String remark;

    public OrgStructure toEntity() {
        OrgStructure org = new OrgStructure();
        org.setName(this.name);
        org.setParentId(this.parentId);
        org.setRemark(this.remark);
        return org;
    }
}

