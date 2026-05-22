package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.WorkOrderItem;

@Repository
public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, Long> {
    
    List<WorkOrderItem> findByWorkOrderId(Long workOrderId);
    
    List<WorkOrderItem> findByWorkOrderIdAndItemType(Long workOrderId, Integer itemType);
    
    long countByWorkOrderId(Long workOrderId);
    
    long countByWorkOrderIdAndStatus(Long workOrderId, Integer status);
    
    List<WorkOrderItem> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Report aggregation queries
    @Query("SELECT i.itemType, COUNT(i) FROM WorkOrderItem i WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.itemType")
    List<Object[]> countGroupByTypeBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(i) FROM WorkOrderItem i WHERE i.createdAt BETWEEN :start AND :end")
    long countBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}


