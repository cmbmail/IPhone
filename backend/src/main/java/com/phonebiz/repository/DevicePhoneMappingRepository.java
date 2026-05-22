package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.DevicePhoneMapping;

@Repository
public interface DevicePhoneMappingRepository extends JpaRepository<DevicePhoneMapping, Long> {
    List<DevicePhoneMapping> findByPhoneDeviceId(Long deviceId);
    List<DevicePhoneMapping> findByPhoneId(Long phoneId);
    Optional<DevicePhoneMapping> findByPhoneDeviceIdAndPhoneId(Long deviceId, Long phoneId);
    boolean existsByPhoneDeviceIdAndPhoneId(Long deviceId, Long phoneId);
    void deleteByPhoneDeviceId(Long deviceId);
    void deleteByPhoneDeviceIdAndPhoneId(Long deviceId, Long phoneId);

    @org.springframework.data.jpa.repository.Query("SELECT m.phoneDeviceId, COUNT(m) FROM DevicePhoneMapping m WHERE m.phoneDeviceId IN :deviceIds GROUP BY m.phoneDeviceId")
    List<Object[]> countByPhoneDeviceIdIn(@org.springframework.data.repository.query.Param("deviceIds") List<Long> deviceIds);
}

