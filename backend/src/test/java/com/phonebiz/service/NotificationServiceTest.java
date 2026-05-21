package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.phonebiz.entity.Notification;
import com.phonebiz.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .userId(1L)
                .notificationType(Notification.NotificationType.PHONE_ALLOCATED)
                .title("Test Notification")
                .content("Test Content")
                .status(Notification.NotificationStatus.UNREAD)
                .build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("测试创建通知")
    void testCreateNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.createNotification(1L,
                Notification.NotificationType.PHONE_ALLOCATED,
                "Title", "Content", 100L, "PhoneNumber");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(Notification.NotificationType.PHONE_ALLOCATED, result.getNotificationType());
        assertEquals(Notification.NotificationStatus.UNREAD, result.getStatus());
    }

    @Test
    @DisplayName("测试标记为已读")
    void testMarkAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markAsRead(1L);

        assertEquals(Notification.NotificationStatus.READ, result.getStatus());
        assertNotNull(result.getReadAt());
    }

    @Test
    @DisplayName("测试标记为已读 - 已是已读状态")
    void testMarkAsRead_AlreadyRead() {
        notification.setStatus(Notification.NotificationStatus.READ);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        Notification result = notificationService.markAsRead(1L);

        assertEquals(Notification.NotificationStatus.READ, result.getStatus());
    }

    @Test
    @DisplayName("测试全部标记为已读")
    void testMarkAllAsRead() {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notification);

        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L,
                Notification.NotificationStatus.UNREAD)).thenReturn(notifications);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markAllAsRead(1L);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("测试获取未读数量")
    void testCountUnread() {
        when(notificationRepository.countByUserIdAndStatus(1L,
                Notification.NotificationStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.countUnread(1L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("测试归档通知")
    void testArchiveNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.archiveNotification(1L);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("测试删除通知")
    void testDeleteNotification() {
        doNothing().when(notificationRepository).deleteById(1L);

        assertDoesNotThrow(() -> notificationService.deleteNotification(1L));
        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试发送号码分配通知")
    void testSendPhoneAllocatedNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendPhoneAllocatedNotification(1L, "13800138000");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("测试发送设备离线通知")
    void testSendDeviceOfflineNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendDeviceOfflineNotification(1L, "device-001");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("测试获取通知列表")
    void testGetNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<Notification> result = notificationService.getNotifications(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("测试按状态获取通知")
    void testGetNotificationsByStatus() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdAndStatus(1L, Notification.NotificationStatus.UNREAD, pageable)).thenReturn(page);

        Page<Notification> result = notificationService.getNotificationsByStatus(1L, Notification.NotificationStatus.UNREAD, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("测试清理已归档通知")
    void testCleanArchived() {
        doNothing().when(notificationRepository).deleteByUserIdAndStatus(1L, Notification.NotificationStatus.ARCHIVED);

        assertDoesNotThrow(() -> notificationService.cleanArchived(1L));
        verify(notificationRepository, times(1)).deleteByUserIdAndStatus(1L, Notification.NotificationStatus.ARCHIVED);
    }
}

