package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.phonebiz.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    List<Notification> findByUserIdAndStatus(Long userId, Integer status);

    long countByUserIdAndStatus(Long userId, Integer status);
}
