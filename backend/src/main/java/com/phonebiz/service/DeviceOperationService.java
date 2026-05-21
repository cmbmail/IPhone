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
import com.phonebiz.entity.DeviceOperation;
import com.phonebiz.repository.DeviceOperationRepository;
import com.phonebiz.repository.DeviceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOperationService {

    private final DeviceOperationRepository operationRepository;
    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public Page<DeviceOperation> getOperations(Pageable pageable) {
        return operationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public DeviceOperation getOperationById(Long id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002));
    }

    @Transactional(readOnly = true)
    public List<DeviceOperation> getOperationsByDeviceId(String deviceId) {
        return operationRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

    @Transactional
    public DeviceOperation createOperation(String deviceId, DeviceOperation.OperationType operationType, 
                                           String params, String operator) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_001));

        DeviceOperation operation = DeviceOperation.builder()
                .deviceId(deviceId)
                .operationType(operationType)
                .status(DeviceOperation.OperationStatus.PENDING)
                .params(params)
                .operator(operator)
                .build();

        operation.setCreatedAt(LocalDateTime.now());
        operation.setUpdatedAt(LocalDateTime.now());

        return operationRepository.save(operation);
    }

    @Transactional
    public DeviceOperation executeOperation(Long operationId) {
        DeviceOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002));

        if (operation.getStatus() != DeviceOperation.OperationStatus.PENDING) {
            throw new BusinessException(ErrorCode.SYS_002);
        }

        operation.setStatus(DeviceOperation.OperationStatus.PROCESSING);
        operation.setUpdatedAt(LocalDateTime.now());
        operationRepository.save(operation);

        try {
            boolean success = simulateOperation(operation.getDeviceId(), operation.getOperationType());
            
            if (success) {
                operation.setStatus(DeviceOperation.OperationStatus.COMPLETED);
                operation.setResult("Operation completed successfully");
            } else {
                operation.setStatus(DeviceOperation.OperationStatus.FAILED);
                operation.setErrorMessage("Operation failed");
            }
        } catch (Exception e) {
            operation.setStatus(DeviceOperation.OperationStatus.FAILED);
            operation.setErrorMessage(e.getMessage());
            log.error("Operation {} failed: {}", operationId, e.getMessage());
        }

        operation.setUpdatedAt(LocalDateTime.now());
        return operationRepository.save(operation);
    }

    private boolean simulateOperation(String deviceId, DeviceOperation.OperationType operationType) {
        // TODO: Replace with real device API integration
        log.warn("MOCK: Device operation {} for {} is using simulated implementation", operationType, deviceId);
        log.info("Executing {} operation for device {}", operationType, deviceId);
        return true;
    }

    @Transactional
    public DeviceOperation rebootDevice(String deviceId, String operator) {
        return createOperation(deviceId, DeviceOperation.OperationType.REBOOT, null, operator);
    }

    @Transactional
    public DeviceOperation syncConfig(String deviceId, String operator) {
        return createOperation(deviceId, DeviceOperation.OperationType.CONFIG_SYNC, null, operator);
    }

    @Transactional
    public DeviceOperation upgradeFirmware(String deviceId, String firmwareVersion, String operator) {
        return createOperation(deviceId, DeviceOperation.OperationType.FIRMWARE_UPGRADE, 
                "{\"firmwareVersion\": \"" + firmwareVersion + "\"}", operator);
    }

    @Transactional
    public DeviceOperation factoryReset(String deviceId, String operator) {
        return createOperation(deviceId, DeviceOperation.OperationType.FACTORY_RESET, null, operator);
    }

    @Transactional
    public DeviceOperation registerDevice(String deviceId, String operator) {
        return createOperation(deviceId, DeviceOperation.OperationType.REGISTER, null, operator);
    }
}

