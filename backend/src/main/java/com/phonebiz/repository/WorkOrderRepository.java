package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.WorkOrder;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    
    Optional<WorkOrder> findByWorkOrderNo(String workOrderNo);
    
    Page<WorkOrder> findByStatus(WorkOrder.WorkOrderStatus status, Pageable pageable);
    
    Page<WorkOrder> findByRequesterId(Long requesterId, Pageable pageable);
    
    Page<WorkOrder> findByHandlerId(Long handlerId, Pageable pageable);
    
    List<WorkOrder> findByBatchId(String batchId);
    
    boolean existsByWorkOrderNo(String workOrderNo);
    
    long countByWorkOrderNoStartingWith(String prefix);
    
    List<WorkOrder> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}

