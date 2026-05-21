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
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
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
}
