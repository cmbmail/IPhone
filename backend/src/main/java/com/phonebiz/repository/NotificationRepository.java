package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.phonebiz.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findBySysUserId(Long userId, Pageable pageable);

    Page<Notification> findBySysUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    List<Notification> findBySysUserIdAndStatus(Long userId, Integer status);

    long countBySysUserIdAndStatus(Long userId, Integer status);
}
