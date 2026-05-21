package com.phonebiz.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneHistory;

@Repository
public interface PhoneHistoryRepository extends JpaRepository<PhoneHistory, Long> {

    Page<PhoneHistory> findByPhoneIdOrderByOperatedAtDesc(Long phoneId, Pageable pageable);

    List<PhoneHistory> findByWorkOrderNo(String workOrderNo);

    List<PhoneHistory> findByPhoneNumberOrderByOperatedAtDesc(String phoneNumber);
    
    long countByActionAndOperatedAtBetween(String action, LocalDateTime start, LocalDateTime end);
}

