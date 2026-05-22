package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.phonebiz.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification")
public class Notification extends BaseEntity {

    // Status constants
    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;
    public static final int STATUS_ARCHIVED = 2;

    // Type constants
    public static final int TYPE_PHONE_ALLOCATED = 1;
    public static final int TYPE_PHONE_SURRENDERED = 2;
    public static final int TYPE_PHONE_STATUS_CHANGED = 3;
    public static final int TYPE_DEVICE_OFFLINE = 4;
    public static final int TYPE_DEVICE_ONLINE = 5;
    public static final int TYPE_BILL_OVERDUE = 6;
    public static final int TYPE_SYSTEM_ALERT = 7;
    public static final int TYPE_WORK_ORDER_ASSIGNED = 8;
    public static final int TYPE_IMPORT_COMPLETED = 9;

    @Column(name = "sys_user_id", nullable = false)
    private Long sysUserId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
