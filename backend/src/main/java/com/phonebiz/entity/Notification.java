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
@Table(name = "notification")
public class Notification extends BaseEntity {

    public static final int NOTIF_PHONE_ALLOCATED = 1;
    public static final int NOTIF_PHONE_SURRENDERED = 2;
    public static final int NOTIF_PHONE_STATUS_CHANGED = 3;
    public static final int NOTIF_DEVICE_OFFLINE = 4;
    public static final int NOTIF_DEVICE_ONLINE = 5;
    public static final int NOTIF_BILL_OVERDUE = 6;
    public static final int NOTIF_SYSTEM_ALERT = 7;
    public static final int NOTIF_WORK_ORDER_ASSIGNED = 8;
    public static final int NOTIF_IMPORT_COMPLETED = 9;


    public static final int NOTIF_UNREAD = 0;
    public static final int NOTIF_READ = 1;
    public static final int NOTIF_ARCHIVED = 2;


    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "read_at")
    private java.time.LocalDateTime readAt;

    public enum NotificationType {
        PHONE_ALLOCATED,
        PHONE_SURRENDERED,
        PHONE_STATUS_CHANGED,
        DEVICE_OFFLINE,
        DEVICE_ONLINE,
        BILL_OVERDUE,
        SYSTEM_ALERT,
        WORK_ORDER_ASSIGNED,
        IMPORT_COMPLETED
    }

    public enum NotificationStatus {
        UNREAD,
        READ,
        ARCHIVED
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
