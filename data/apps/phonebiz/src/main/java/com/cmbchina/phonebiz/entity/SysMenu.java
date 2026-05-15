package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long parentId;

    private String menuName;

    private String menuCode;

    private String menuType;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer status;

    private String remark;
}
