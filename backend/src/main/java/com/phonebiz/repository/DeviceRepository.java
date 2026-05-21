package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    Optional<Device> findByDeviceId(String deviceId);
    
    Optional<Device> findByMacAddress(String macAddress);
    
    List<Device> findByPhoneNumber(String phoneNumber);
    
    List<Device> findByStatus(Device.DeviceStatus status);
    
    Page<Device> findByDeviceType(Device.DeviceType deviceType, Pageable pageable);
    
    boolean existsByDeviceId(String deviceId);
    
    boolean existsByMacAddress(String macAddress);
}

