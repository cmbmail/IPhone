package com.phonebiz.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.ImportBatch;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    
    Optional<ImportBatch> findByBatchId(String batchId);
    
    Page<ImportBatch> findByOperator(String operator, Pageable pageable);
}

