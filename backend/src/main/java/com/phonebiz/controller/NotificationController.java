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
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.Notification;
import com.phonebiz.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<Notification>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<Notification>> getNotificationsByStatus(
            @PathVariable Integer status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(notificationService.getNotificationsByStatus(userId, status, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", notificationService.countUnread(userId));
        return ApiResponse.success(result);
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Notification> markAsRead(@PathVariable Long notificationId) {
        verifyOwnership(notificationId);
        return ApiResponse.success(notificationService.markAsRead(notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("All marked as read", null);
    }

    @PostMapping("/{notificationId}/archive")
    public ApiResponse<Void> archiveNotification(@PathVariable Long notificationId) {
        verifyOwnership(notificationId);
        notificationService.archiveNotification(notificationId);
        return ApiResponse.success("Notification archived", null);
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long notificationId) {
        verifyOwnership(notificationId);
        notificationService.deleteNotification(notificationId);
        return ApiResponse.success("Notification deleted", null);
    }

    @DeleteMapping("/clean-archived")
    public ApiResponse<Void> cleanArchived() {
        Long userId = getCurrentUserId();
        notificationService.cleanArchived(userId);
        return ApiResponse.success("Archived notifications cleaned", null);
    }

    /** Get current user ID from JWT authentication */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new BusinessException(ErrorCode.AUTH_004);
        return notificationService.getUserIdByUsername(auth.getName());
    }

    /** Verify notification ownership to prevent IDOR */
    private void verifyOwnership(Long notificationId) {
        Long currentUserId = getCurrentUserId();
        Notification notification = notificationService.getNotificationById(notificationId);
        if (notification == null || !notification.getSysUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.SYS_002, "Notification not found or access denied");
        }
    }
}
