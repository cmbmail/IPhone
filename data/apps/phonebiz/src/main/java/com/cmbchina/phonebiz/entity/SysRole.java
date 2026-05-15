package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String roleName;

    private String roleCode;

    private Integer sort;

    private Integer status;

    private String remark;
}
