package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.WorkOrder;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    
    Optional<WorkOrder> findByWorkOrderNo(String workOrderNo);
    
    Page<WorkOrder> findByStatus(Integer status, Pageable pageable);
    
    Page<WorkOrder> findByRequesterId(Long requesterId, Pageable pageable);
    
    Page<WorkOrder> findByHandlerId(Long handlerId, Pageable pageable);
    
    List<WorkOrder> findByBatchId(String batchId);
    
    boolean existsByWorkOrderNo(String workOrderNo);
    
    long countByWorkOrderNoStartingWith(String prefix);
    
    List<WorkOrder> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Report aggregation queries
    @Query("SELECT o.status, COUNT(o) FROM WorkOrder o WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.status")
    List<Object[]> countGroupByStatusBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(o) FROM WorkOrder o WHERE o.createdAt BETWEEN :start AND :end")
    long countBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT w FROM WorkOrder w WHERE w.workOrderNo LIKE %:keyword% OR w.title LIKE %:keyword% OR w.description LIKE %:keyword%")
    List<WorkOrder> searchByKeyword(@Param("keyword") String keyword);

}


