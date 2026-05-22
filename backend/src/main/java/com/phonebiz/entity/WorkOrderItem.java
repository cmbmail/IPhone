package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "work_order_item")
public class WorkOrderItem extends BaseEntity {

    public static final int ITEM_PENDING = 0;
    public static final int ITEM_PROCESSING = 1;
    public static final int ITEM_COMPLETED = 2;
    public static final int ITEM_FAILED = 3;
    public static final int ITEM_SKIPPED = 4;


    public static final int ITEM_PHONE = 1;
    public static final int ITEM_DEVICE = 2;
    public static final int ITEM_EMPLOYEE = 3;


    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;
    @Column(name = "item_type", nullable = false, length = 20)
    private Integer itemType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "from_value", length = 500)
    private String fromValue;

    @Column(name = "to_value", length = 500)
    private String toValue;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;

    @Column(name = "executed_at")
    private java.time.LocalDateTime executedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;
}
