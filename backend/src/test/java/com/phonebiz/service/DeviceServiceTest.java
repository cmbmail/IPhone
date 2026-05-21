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
import com.phonebiz.repository.DeviceRepository;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .deviceId("device-001")
                .deviceName("Test Phone")
                .deviceType(Device.DeviceType.IP_PHONE)
                .model("SIP-T46G")
                .macAddress("00:11:22:33:44:55")
                .status(Device.DeviceStatus.UNREGISTERED)
                .build();
    }

    @Test
    void getDeviceById_ShouldReturnDevice() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        Device result = deviceService.getDeviceById(1L);

        assertNotNull(result);
        assertEquals("device-001", result.getDeviceId());
    }

    @Test
    void getDeviceById_ShouldThrowException_WhenNotFound() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> deviceService.getDeviceById(1L));
    }

    @Test
    void createDevice_ShouldCreateNewDevice() {
        when(deviceRepository.existsByDeviceId("device-001")).thenReturn(false);
        when(deviceRepository.existsByMacAddress("00:11:22:33:44:55")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.createDevice(device);

        assertNotNull(result);
        assertEquals(Device.DeviceStatus.UNREGISTERED, result.getStatus());
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void createDevice_ShouldThrowException_WhenDeviceIdExists() {
        when(deviceRepository.existsByDeviceId("device-001")).thenReturn(true);

        assertThrows(BusinessException.class, () -> deviceService.createDevice(device));
    }

    @Test
    void createDevice_ShouldThrowException_WhenMacAddressExists() {
        when(deviceRepository.existsByDeviceId("device-001")).thenReturn(false);
        when(deviceRepository.existsByMacAddress("00:11:22:33:44:55")).thenReturn(true);

        assertThrows(BusinessException.class, () -> deviceService.createDevice(device));
    }

    @Test
    void updateDevice_ShouldUpdateDevice() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device updateData = new Device();
        updateData.setDeviceName("Updated Name");

        Device result = deviceService.updateDevice(1L, updateData);

        assertEquals("Updated Name", result.getDeviceName());
    }

    @Test
    void updateDevice_ShouldThrowException_WhenNotFound() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

        Device updateData = new Device();
        updateData.setDeviceName("Updated Name");

        assertThrows(BusinessException.class, () -> deviceService.updateDevice(1L, updateData));
    }

    @Test
    void deleteDevice_ShouldThrowException_WhenNotFound() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> deviceService.deleteDevice(1L));
    }

    @Test
    void updateStatus_ShouldThrowException_WhenDeviceNotFound() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> 
            deviceService.updateStatus("device-001", Device.DeviceStatus.ONLINE));
    }

    @Test
    void checkin_ShouldThrowException_WhenDeviceNotFound() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> 
            deviceService.checkin("device-001", "192.168.1.100", "1.0.0"));
    }

    @Test
    void existsByDeviceId_ShouldReturnTrue_WhenExists() {
        when(deviceRepository.existsByDeviceId("device-001")).thenReturn(true);

        assertTrue(deviceRepository.existsByDeviceId("device-001"));
    }

    @Test
    void existsByDeviceId_ShouldReturnFalse_WhenNotExists() {
        when(deviceRepository.existsByDeviceId("device-001")).thenReturn(false);

        assertFalse(deviceRepository.existsByDeviceId("device-001"));
    }

    @Test
    void updateStatus_ShouldUpdateStatus() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.updateStatus("device-001", Device.DeviceStatus.ONLINE);

        assertEquals(Device.DeviceStatus.ONLINE, result.getStatus());
        assertNotNull(result.getLastCheckinTime());
    }

    @Test
    void checkin_ShouldUpdateDeviceInfo() {
        when(deviceRepository.findByDeviceId("device-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.checkin("device-001", "192.168.1.100", "1.0.0");

        assertEquals(Device.DeviceStatus.ONLINE, result.getStatus());
        assertEquals("192.168.1.100", result.getIpAddress());
        assertEquals("1.0.0", result.getFirmwareVersion());
    }
}

