package com.phonebiz.entity;

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

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "from_value", length = 500)
    private String fromValue;

    @Column(name = "to_value", length = 500)
    private String toValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status;

    @Column(name = "executed_at")
    private java.time.LocalDateTime executedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "remark", length = 500)
    private String remark;

    public enum ItemType {
        PHONE,
        DEVICE,
        EMPLOYEE
    }

    public enum ItemStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        SKIPPED
    }
}
