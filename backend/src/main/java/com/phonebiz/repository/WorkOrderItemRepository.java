package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.WorkOrderItem;

@Repository
public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, Long> {
    
    List<WorkOrderItem> findByWorkOrderId(Long workOrderId);
    
    List<WorkOrderItem> findByWorkOrderIdAndItemType(Long workOrderId, WorkOrderItem.ItemType itemType);
    
    long countByWorkOrderId(Long workOrderId);
    
    long countByWorkOrderIdAndStatus(Long workOrderId, WorkOrderItem.ItemStatus status);
    
    List<WorkOrderItem> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}

