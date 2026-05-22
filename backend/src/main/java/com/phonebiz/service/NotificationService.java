package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
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

    public Long getUserIdByUsername(String username) {
        return sysUserRepository.findByUsername(username)
                .map(SysUser::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findBySysUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByStatus(Long userId, Integer status, Pageable pageable) {
        return notificationRepository.findBySysUserIdAndStatus(userId, status, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countBySysUserIdAndStatus(userId, Notification.STATUS_UNREAD);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002));
        notification.setStatus(Notification.STATUS_READ);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findBySysUserIdAndStatus(userId, Notification.STATUS_UNREAD);
        for (Notification n : unread) {
            n.setStatus(Notification.STATUS_READ);
            n.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void archiveNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002));
        notification.setStatus(Notification.STATUS_ARCHIVED);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(e -> { e.setDeletedAt(LocalDateTime.now()); notificationRepository.save(e); });
    }

    @Transactional
    public void cleanArchived(Long userId) {
        List<Notification> archived = notificationRepository.findBySysUserIdAndStatus(userId, Notification.STATUS_ARCHIVED);
        archived.forEach(e -> e.setDeletedAt(LocalDateTime.now())); notificationRepository.saveAll(archived);
    }

    @Transactional(readOnly = true)
    public Notification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }

    @Transactional
    public Notification createNotification(Long sysUserId, Integer type, String title, String content, Long sourceId, String sourceType) {
        Notification notification = Notification.builder()
                .sysUserId(sysUserId)
                .type(type)
                .title(title)
                .content(content)
                .status(Notification.STATUS_UNREAD)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build();
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }
}
