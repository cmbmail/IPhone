package com.phonebiz.controller;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.Notification;
import com.phonebiz.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<Notification>> getNotifications(
            @RequestParam Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<Notification>> getNotificationsByStatus(
            @RequestParam Long userId,
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Notification.NotificationStatus notificationStatus = Notification.NotificationStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(notificationService.getNotificationsByStatus(userId, notificationStatus, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(@RequestParam Long userId) {
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", notificationService.countUnread(userId));
        return ApiResponse.success(result);
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable Long notificationId) {
        return ApiResponse.success(notificationService.markAsRead(notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestParam Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("All marked as read", null);
    }

    @PostMapping("/{notificationId}/archive")
    public ApiResponse<Void> archiveNotification(@PathVariable Long notificationId) {
        notificationService.archiveNotification(notificationId);
        return ApiResponse.success("Notification archived", null);
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ApiResponse.success("Notification deleted", null);
    }

    @DeleteMapping("/clean-archived")
    public ApiResponse<Void> cleanArchived(@RequestParam Long userId) {
        notificationService.cleanArchived(userId);
        return ApiResponse.success("Archived notifications cleaned", null);
    }
}

