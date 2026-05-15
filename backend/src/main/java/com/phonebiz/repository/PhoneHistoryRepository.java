package com.phonebiz.repository;

import com.phonebiz.entity.PhoneHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneHistoryRepository extends JpaRepository<PhoneHistory, Long> {

    Page<PhoneHistory> findByPhoneIdOrderByOperatedAtDesc(Long phoneId, Pageable pageable);

    List<PhoneHistory> findByWorkOrderNo(String workOrderNo);

    List<PhoneHistory> findByPhoneNumberOrderByOperatedAtDesc(String phoneNumber);
}
