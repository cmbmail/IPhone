package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.DevicePhoneMapping;

@Repository
public interface DevicePhoneMappingRepository extends JpaRepository<DevicePhoneMapping, Long> {
    List<DevicePhoneMapping> findByDeviceId(Long deviceId);
    List<DevicePhoneMapping> findByPhoneId(Long phoneId);
    Optional<DevicePhoneMapping> findByDeviceIdAndPhoneId(Long deviceId, Long phoneId);
    boolean existsByDeviceIdAndPhoneId(Long deviceId, Long phoneId);
    void deleteByDeviceId(Long deviceId);
    void deleteByDeviceIdAndPhoneId(Long deviceId, Long phoneId);
}

