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
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {

    public static final int WO_LOW = 1;
    public static final int WO_NORMAL = 2;
    public static final int WO_HIGH = 3;
    public static final int WO_URGENT = 4;


    public static final int WO_PENDING = 0;
    public static final int WO_ACCEPTED = 1;
    public static final int WO_PROCESSING = 2;
    public static final int WO_COMPLETED = 3;
    public static final int WO_ARCHIVED = 4;
    public static final int WO_CANCELLED = 5;


    public static final int WO_PHONE_ALLOCATE = 1;
    public static final int WO_PHONE_TRANSFER = 2;
    public static final int WO_PHONE_CHANGE_NUMBER = 3;
    public static final int WO_PHONE_CHANGE_ORG = 4;
    public static final int WO_PHONE_RECLAIM = 5;
    public static final int WO_PHONE_SURRENDER = 6;
    public static final int WO_PHONE_ENABLE = 7;
    public static final int WO_PHONE_DISABLE = 8;


    @Column(name = "work_order_no", unique = true, nullable = false, length = 50)
    private String workOrderNo;
    @Column(name = "type", nullable = false, length = 30)
    private Integer type;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;
    @Column(name = "priority", nullable = false, length = 10)
    private Integer priority;

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
}
