package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_employee_role")
public class SysEmployeeRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long employeeId;

    private Long roleId;
}
