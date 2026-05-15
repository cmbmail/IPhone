package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long parentId;

    private String orgName;

    private String orgCode;

    private Integer orgLevel;

    private Integer sort;

    private String contact;

    private String phone;

    private String address;

    private Integer status;

    private String remark;
}
