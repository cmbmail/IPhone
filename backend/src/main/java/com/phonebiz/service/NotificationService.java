package com.phonebiz.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.entity.Notification;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.NotificationRepository;
import com.phonebiz.repository.SysUserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SysUserRepository sysUserRepository;

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByStatus(Long userId, Notification.NotificationStatus status, 
                                                       Pageable pageable) {
        return notificationRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, Notification.NotificationStatus.UNREAD);
    }

    @Transactional
    public Notification createNotification(Long userId, Notification.NotificationType type, 
                                           String title, String content, Long sourceId, String sourceType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(type)
                .title(title)
                .content(content)
                .status(Notification.NotificationStatus.UNREAD)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build();

        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getStatus() == Notification.NotificationStatus.UNREAD) {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        }
        return notification;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, 
                Notification.NotificationStatus.UNREAD).forEach(n -> {
            n.setStatus(Notification.NotificationStatus.READ);
            n.setReadAt(LocalDateTime.now());
            n.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void archiveNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setStatus(Notification.NotificationStatus.ARCHIVED);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void cleanArchived(Long userId) {
        notificationRepository.deleteByUserIdAndStatus(userId, Notification.NotificationStatus.ARCHIVED);
    }

    @Transactional(readOnly = true)
    public Long getUserIdByUsername(String username) {
        return sysUserRepository.findByUsername(username)
                .map(SysUser::getId)
                .orElseThrow(() -> new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.AUTH_004));
    }

    @Transactional(readOnly = true)
    public Notification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }

    public void sendPhoneAllocatedNotification(Long userId, String phoneNumber) {
        createNotification(userId, Notification.NotificationType.PHONE_ALLOCATED,
                "号码已分配",
                "您已成功分配号码: " + phoneNumber,
                null, "PhoneNumber");
    }

    public void sendPhoneSurrenderedNotification(Long userId, String phoneNumber) {
        createNotification(userId, Notification.NotificationType.PHONE_SURRENDERED,
                "号码已回收",
                "号码 " + phoneNumber + " 已被回收",
                null, "PhoneNumber");
    }

    public void sendDeviceOfflineNotification(Long userId, String deviceId) {
        createNotification(userId, Notification.NotificationType.DEVICE_OFFLINE,
                "设备离线",
                "设备 " + deviceId + " 已离线",
                null, "Device");
    }

    public void sendDeviceOnlineNotification(Long userId, String deviceId) {
        createNotification(userId, Notification.NotificationType.DEVICE_ONLINE,
                "设备上线",
                "设备 " + deviceId + " 已上线",
                null, "Device");
    }

    public void sendImportCompletedNotification(Long userId, String batchId, int successCount, int failCount) {
        String content = String.format("批量导入完成，成功: %d, 失败: %d", successCount, failCount);
        createNotification(userId, Notification.NotificationType.IMPORT_COMPLETED,
                "导入完成", content, null, "ImportBatch");
    }
}

