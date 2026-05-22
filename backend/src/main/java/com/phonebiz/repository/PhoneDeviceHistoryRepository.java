package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneDeviceHistory;

@Repository
public interface PhoneDeviceHistoryRepository extends JpaRepository<PhoneDeviceHistory, Long> {
    List<PhoneDeviceHistory> findByPhoneDeviceIdOrderByOperatedAtDesc(Long deviceId);
    Page<PhoneDeviceHistory> findByPhoneDeviceIdOrderByOperatedAtDesc(Long deviceId, Pageable pageable);
}

