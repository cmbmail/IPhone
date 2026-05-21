package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.CreatePhoneDeviceRequest;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.repository.*;

@ExtendWith(MockitoExtension.class)
class PhoneDeviceServiceTest {

    @Mock
    private PhoneDeviceRepository phoneDeviceRepository;

    @Mock
    private DevicePhoneMappingRepository devicePhoneMappingRepository;

    @Mock
    private PhoneDeviceHistoryRepository phoneDeviceHistoryRepository;

    @Mock
    private PhoneNumberRepository phoneNumberRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrgStructureRepository orgStructureRepository;

    @InjectMocks
    private PhoneDeviceService phoneDeviceService;

    private PhoneDevice testDevice;

    @BeforeEach
    void setUp() {
        testDevice = PhoneDevice.builder()
                .id(1L)
                .macAddress("00:11:22:33:44:55")
                .status(PhoneDevice.PhoneDeviceStatus.stock)
                .build();
    }

    @Test
    @DisplayName("测试获取设备详情 - 成功")
    void testGetDeviceDetail_Success() {
        when(phoneDeviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

        var result = phoneDeviceService.getDeviceDetail(1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("测试获取设备详情 - 不存在")
    void testGetDeviceDetail_NotFound() {
        when(phoneDeviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> phoneDeviceService.getDeviceDetail(99L));
    }

    @Test
    @DisplayName("测试创建设备 - 成功")
    void testCreateDevice_Success() {
        CreatePhoneDeviceRequest request = new CreatePhoneDeviceRequest();
        request.setMacAddress("00:11:22:33:44:55");
        request.setModel("SIP-T46G");
        request.setOrgId(1L);

        when(phoneDeviceRepository.existsByMacAddress(anyString())).thenReturn(false);
        when(orgStructureRepository.existsById(1L)).thenReturn(true);
        when(phoneDeviceRepository.save(any(PhoneDevice.class))).thenReturn(testDevice);

        var result = phoneDeviceService.createDevice(request);

        assertNotNull(result);
        verify(phoneDeviceRepository).save(any(PhoneDevice.class));
    }

    @Test
    @DisplayName("测试创建设备 - MAC已存在")
    void testCreateDevice_MacExists() {
        CreatePhoneDeviceRequest request = new CreatePhoneDeviceRequest();
        request.setMacAddress("00:11:22:33:44:55");
        request.setOrgId(1L);

        when(phoneDeviceRepository.existsByMacAddress("001122334455")).thenReturn(true);

        assertThrows(BusinessException.class, () -> phoneDeviceService.createDevice(request));
    }

    @Test
    @DisplayName("测试获取设备绑定的号码 - 成功")
    void testGetBoundPhones_Success() {
        when(phoneDeviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(devicePhoneMappingRepository.findByDeviceId(1L)).thenReturn(Arrays.asList());

        var result = phoneDeviceService.getBoundPhones(1L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("测试获取设备绑定的号码 - 设备不存在")
    void testGetBoundPhones_DeviceNotFound() {
        when(phoneDeviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> phoneDeviceService.getBoundPhones(99L));
    }

    @Test
    @DisplayName("测试获取设备历史记录 - 成功")
    void testGetDeviceHistory_Success() {
        when(phoneDeviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(phoneDeviceHistoryRepository.findByDeviceIdOrderByOperatedAtDesc(1L)).thenReturn(Arrays.asList());

        var result = phoneDeviceService.getDeviceHistory(1L);

        assertNotNull(result);
    }
}

