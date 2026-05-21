package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.DeviceOperation;

@Repository
public interface DeviceOperationRepository extends JpaRepository<DeviceOperation, Long> {
    
    List<DeviceOperation> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    
    Page<DeviceOperation> findByOperationType(DeviceOperation.OperationType operationType, Pageable pageable);
    
    Page<DeviceOperation> findByStatus(DeviceOperation.OperationStatus status, Pageable pageable);
    
    List<DeviceOperation> findByStatus(DeviceOperation.OperationStatus status);
}

