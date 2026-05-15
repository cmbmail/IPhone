package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("phone_number")
public class PhoneNumber extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String phoneNumber;

    private String status;

    private Long employeeId;

    private String employeeName;

    private String remark;
}
