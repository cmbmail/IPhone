package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.Notification;
import com.phonebiz.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("测试获取通知列表")
    void testGetNotifications() throws Exception {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setTitle("Test Notification");
        notification.setStatus(Notification.NotificationStatus.UNREAD);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationService.getNotifications(anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/notifications").param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试按状态获取通知")
    void testGetNotificationsByStatus() throws Exception {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setTitle("Test Notification");
        notification.setStatus(Notification.NotificationStatus.READ);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationService.getNotificationsByStatus(anyLong(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/notifications/status/READ").param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取未读数量")
    void testGetUnreadCount() throws Exception {
        when(notificationService.countUnread(1L)).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("测试标记为已读")
    void testMarkAsRead() throws Exception {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setStatus(Notification.NotificationStatus.READ);

        when(notificationService.markAsRead(1L)).thenReturn(notification);

        mockMvc.perform(post("/notifications/{notificationId}/read", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试全部标记为已读")
    void testMarkAllAsRead() throws Exception {
        doNothing().when(notificationService).markAllAsRead(1L);

        mockMvc.perform(post("/notifications/read-all").param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试归档通知")
    void testArchiveNotification() throws Exception {
        doNothing().when(notificationService).archiveNotification(1L);

        mockMvc.perform(post("/notifications/{notificationId}/archive", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除通知")
    void testDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L);

        mockMvc.perform(delete("/notifications/{notificationId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试清理已归档通知")
    void testCleanArchived() throws Exception {
        doNothing().when(notificationService).cleanArchived(1L);

        mockMvc.perform(delete("/notifications/clean-archived").param("userId", "1"))
                .andExpect(status().isOk());
    }
}

