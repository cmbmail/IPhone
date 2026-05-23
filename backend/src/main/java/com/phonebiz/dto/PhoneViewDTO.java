package com.phonebiz.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 号码资源视图DTO - 一个电话号码可关联多个分机号和MAC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneViewDTO {

    private Long id;
    private String phoneNumber;
    private String employeeNo;
    private String employeeName;

    /** 该电话号码关联的所有分机号 */
    private List<String> extensions;

    /** 该电话号码关联的所有MAC地址(通过分机号→设备映射) */
    private List<String> macAddresses;

    /** 分行名称 */
    private String branchName;

    /** 部门名称 */
    private String deptName;

    /**
     * 动态计算状态:
     * 0=闲置(分机号为空)
     * 1=占用(分机号不为空,无未完成工单)
     * 2=分配中(分机号不为空,有未完成工单)
     */
    private Integer status;

    private Long orgId;
}
