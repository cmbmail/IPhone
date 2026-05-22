package com.phonebiz.dto;

import jakarta.validation.constraints.Size;

import lombok.Data;

import java.util.List;

@Data
public class UpdateRoleRequest {

    @Size(max = 50, message = "角色名称不能超过50个字符")
    private String name;

    @Size(max = 200, message = "描述不能超过200个字符")
    private String description;

    private Integer status;

    private List<Long> permissionIds;
}
