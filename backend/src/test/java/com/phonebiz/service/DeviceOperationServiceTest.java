package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phonebiz.common.BusinessException;
import com.phonebiz.entity.Device;
import com.phonebiz.entity.DeviceOperation;
import com.phonebiz.repository.DeviceOperationRepository;
import com.phonebiz.repository.DeviceRepository;

@ExtendWith(MockitoExtension.class)
class DeviceOperationServiceTest {

    @Mock
    private DeviceOperationRepository operationRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceOperationService operationService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .deviceId("device-001")
                .deviceType(Device.DeviceType.IP_PHONE)
                .status(Device.DeviceStatus.ONLINE)
                .build();
    }

    @Test
    void createOperation_ShouldCreateNewOperation() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(operationRepository.save(any(DeviceOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceOperation result = operationService.createOperation("device-001", 
                DeviceOperation.OperationType.REBOOT, null, "admin");

        assertNotNull(result);
        assertEquals("device-001", result.getDeviceId());
        assertEquals(DeviceOperation.OperationType.REBOOT, result.getOperationType());
        assertEquals(DeviceOperation.OperationStatus.PENDING, result.getStatus());
    }

    @Test
    void createOperation_ShouldThrowException_WhenDeviceNotFound() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> 
            operationService.createOperation("device-001", 
                DeviceOperation.OperationType.REBOOT, null, "admin"));
    }

    @Test
    void executeOperation_ShouldExecutePendingOperation() {
        DeviceOperation operation = DeviceOperation.builder()
                .deviceId("device-001")
                .operationType(DeviceOperation.OperationType.REBOOT)
                .status(DeviceOperation.OperationStatus.PENDING)
                .build();

        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));
        when(operationRepository.save(any(DeviceOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceOperation result = operationService.executeOperation(1L);

        assertEquals(DeviceOperation.OperationStatus.COMPLETED, result.getStatus());
        assertEquals("Operation completed successfully", result.getResult());
    }

    @Test
    void executeOperation_ShouldThrowException_WhenNotPending() {
        DeviceOperation operation = DeviceOperation.builder()
                .deviceId("device-001")
                .operationType(DeviceOperation.OperationType.REBOOT)
                .status(DeviceOperation.OperationStatus.COMPLETED)
                .build();

        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));

        assertThrows(BusinessException.class, () -> operationService.executeOperation(1L));
    }

    @Test
    void rebootDevice_ShouldCreateRebootOperation() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(operationRepository.save(any(DeviceOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceOperation result = operationService.rebootDevice("device-001", "admin");

        assertEquals(DeviceOperation.OperationType.REBOOT, result.getOperationType());
    }

    @Test
    void syncConfig_ShouldCreateConfigSyncOperation() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(operationRepository.save(any(DeviceOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceOperation result = operationService.syncConfig("device-001", "admin");

        assertEquals(DeviceOperation.OperationType.CONFIG_SYNC, result.getOperationType());
    }

    @Test
    void upgradeFirmware_ShouldCreateUpgradeOperation() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(operationRepository.save(any(DeviceOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceOperation result = operationService.upgradeFirmware("device-001", "2.0.0", "admin");

        assertEquals(DeviceOperation.OperationType.FIRMWARE_UPGRADE, result.getOperationType());
        assertNotNull(result.getParams());
    }
}

