package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.Device;
import com.phonebiz.repository.DeviceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public Page<Device> getDevices(Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));
    }

    @Transactional(readOnly = true)
    public Device getDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));
    }

    @Transactional
    public Device createDevice(Device device) {
        if (deviceRepository.existsByDeviceId(device.getDeviceId())) {
            throw new BusinessException(ErrorCode.DEVICE_002);
        }
        
        if (device.getMacAddress() != null && deviceRepository.existsByMacAddress(device.getMacAddress())) {
            throw new BusinessException(ErrorCode.DEVICE_003);
        }
        
        device.setStatus(Device.DEV_UNREGISTERED);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }

    @Transactional
    public Device updateDevice(Long id, Device updateData) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));

        if (updateData.getDeviceId() != null && !updateData.getDeviceId().equals(device.getDeviceId())) {
            if (deviceRepository.existsByDeviceId(updateData.getDeviceId())) {
                throw new BusinessException(ErrorCode.DEVICE_002);
            }
            device.setDeviceId(updateData.getDeviceId());
        }

        if (updateData.getMacAddress() != null && !updateData.getMacAddress().equals(device.getMacAddress())) {
            if (deviceRepository.existsByMacAddress(updateData.getMacAddress())) {
                throw new BusinessException(ErrorCode.DEVICE_003);
            }
            device.setMacAddress(updateData.getMacAddress());
        }

        if (updateData.getDeviceName() != null) {
            device.setDeviceName(updateData.getDeviceName());
        }
        if (updateData.getDeviceType() != null) {
            device.setDeviceType(updateData.getDeviceType());
        }
        if (updateData.getModel() != null) {
            device.setModel(updateData.getModel());
        }
        if (updateData.getIpAddress() != null) {
            device.setIpAddress(updateData.getIpAddress());
        }
        if (updateData.getPhoneNumber() != null) {
            device.setPhoneNumber(updateData.getPhoneNumber());
        }
        if (updateData.getExtensionNumber() != null) {
            device.setExtensionNumber(updateData.getExtensionNumber());
        }
        if (updateData.getFirmwareVersion() != null) {
            device.setFirmwareVersion(updateData.getFirmwareVersion());
        }
        if (updateData.getRemark() != null) {
            device.setRemark(updateData.getRemark());
        }

        device.setUpdatedAt(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));
        
        device.setDeletedAt(LocalDateTime.now()); deviceRepository.save(device);
        log.info("Device {} deleted", device.getDeviceId());
    }

    @Transactional
    public Device updateStatus(String deviceId, Integer status) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));
        
        device.setStatus(status);
        
        if (status == Device.DEV_ONLINE) {
            device.setLastCheckinTime(LocalDateTime.now());
        }
        
        device.setUpdatedAt(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public Device checkin(String deviceId, String ipAddress, String firmwareVersion) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));
        
        device.setStatus(Device.DEV_ONLINE);
        device.setIpAddress(ipAddress);
        device.setFirmwareVersion(firmwareVersion);
        device.setLastCheckinTime(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<Device> getDevicesByStatus(Integer status) {
        return deviceRepository.findByStatus(status);
    }
}

