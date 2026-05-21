package com.phonebiz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneSurrenderRecord;

@Repository
public interface PhoneSurrenderRecordRepository extends JpaRepository<PhoneSurrenderRecord, Long> {

    Optional<PhoneSurrenderRecord> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}

