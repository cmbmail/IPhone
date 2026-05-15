package com.cmbchina.phonebiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String orderType;

    private String status;

    private Long phoneId;

    private String phoneNumber;

    private Long requesterId;

    private String requesterName;

    private Long handlerId;

    private String handlerName;

    private String title;

    private String content;

    private String reason;

    private String result;

    private Integer priority;
}
