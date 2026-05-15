package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_employee")
public class SysEmployee extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long orgId;

    private String empNo;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private Integer gender;

    private String idCard;

    private Integer status;

    private String remark;
}
