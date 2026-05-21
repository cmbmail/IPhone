package com.phonebiz.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.AuditLogEntity;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByModule(String module, Pageable pageable);

    Page<AuditLogEntity>findByOperator(String operator, Pageable pageable);

    Page<AuditLogEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:operator IS NULL OR a.operator = :operator) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> searchAuditLogs(
            @Param("module") String module,
            @Param("operator") String operator,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByModuleAndCreatedAtAfter(String module, LocalDateTime since);
}
