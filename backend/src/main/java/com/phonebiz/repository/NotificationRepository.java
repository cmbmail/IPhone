package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndStatus(Long userId, Notification.NotificationStatus status, Pageable pageable);
    
    long countByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
    
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Notification.NotificationStatus status);
    
    void deleteByUserIdAndStatus(Long userId, Notification.NotificationStatus status);
}

