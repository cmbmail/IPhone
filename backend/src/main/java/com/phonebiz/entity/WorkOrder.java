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
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {

    @Column(name = "work_order_no", unique = true, nullable = false, length = 50)
    private String workOrderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private WorkOrderPriority priority;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "requester_name", length = 100)
    private String requesterName;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handler_name", length = 100)
    private String handlerName;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;

    @Column(name = "remark", length = 500)
    private String remark;

    public enum WorkOrderType {
        PHONE_ALLOCATE,
        PHONE_TRANSFER,
        PHONE_CHANGE_NUMBER,
        PHONE_CHANGE_ORG,
        PHONE_RECLAIM,
        PHONE_SURRENDER,
        PHONE_ENABLE,
        PHONE_DISABLE
    }

    public enum WorkOrderStatus {
        PENDING,
        ACCEPTED,
        PROCESSING,
        COMPLETED,
        REJECTED,
        ARCHIVED
    }

    public enum WorkOrderPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
