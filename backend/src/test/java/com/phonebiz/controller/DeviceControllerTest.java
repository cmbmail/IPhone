package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.Device;
import com.phonebiz.service.DeviceService;

@WebMvcTest(DeviceController.class)
@Import(TestSecurityConfig.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviceService deviceService;

    private Device createTestDevice() {
        Device device = new Device();
        device.setId(1L);
        device.setDeviceId("DEV001");
        device.setDeviceName("Test Device");
        device.setDeviceType(Device.DeviceType.IP_PHONE);
        device.setStatus(Device.DeviceStatus.UNREGISTERED);
        return device;
    }

    @Test
    @DisplayName("测试获取设备列表")
    void testGetDevices() throws Exception {
        Page<Device> page = new PageImpl<>(List.of(createTestDevice()));
        when(deviceService.getDevices(any())).thenReturn(page);

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备详情")
    void testGetDeviceById() throws Exception {
        when(deviceService.getDeviceById(1L)).thenReturn(createTestDevice());

        mockMvc.perform(get("/devices/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备")
    void testCreateDevice() throws Exception {
        Device newDevice = createTestDevice();
        when(deviceService.createDevice(any(Device.class))).thenReturn(newDevice);

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDevice)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新设备")
    void testUpdateDevice() throws Exception {
        Device updatedDevice = createTestDevice();
        updatedDevice.setDeviceName("Updated Device");
        when(deviceService.updateDevice(eq(1L), any(Device.class))).thenReturn(updatedDevice);

        Device updateData = new Device();
        updateData.setDeviceName("Updated Device");

        mockMvc.perform(put("/devices/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除设备")
    void testDeleteDevice() throws Exception {
        doNothing().when(deviceService).deleteDevice(1L);

        mockMvc.perform(delete("/devices/{id}", 1L))
                .andExpect(status().isOk());
    }
}

