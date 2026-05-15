package com.phonebiz.repository;

import com.phonebiz.entity.PhoneSurrenderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneSurrenderRecordRepository extends JpaRepository<PhoneSurrenderRecord, Long> {

    Optional<PhoneSurrenderRecord> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
