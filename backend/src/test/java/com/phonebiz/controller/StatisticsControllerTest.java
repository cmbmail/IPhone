package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.Device;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.service.StatisticsService;

@WebMvcTest(StatisticsController.class)
@Import(TestSecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @Test
    @DisplayName("测试获取电话统计")
    void testGetPhoneStatistics() throws Exception {
        when(statisticsService.getPhoneStatistics()).thenReturn(
            new com.phonebiz.dto.PhoneStatisticsDTO());

        mockMvc.perform(get("/statistics/phones"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取组织电话统计")
    void testGetPhoneStatisticsByOrg() throws Exception {
        when(statisticsService.getPhoneStatisticsByOrg(1L)).thenReturn(
            new com.phonebiz.dto.PhoneStatisticsDTO());

        mockMvc.perform(get("/statistics/phones/org/{orgId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试按状态获取电话列表")
    void testGetPhonesByStatus() throws Exception {
        PhoneNumber phone = new PhoneNumber();
        phone.setId(1L);
        phone.setPhoneNumber("13800138000");
        phone.setStatus(PhoneNumber.PhoneStatus.active);

        Page<PhoneNumber> page = new PageImpl<>(List.of(phone));
        when(statisticsService.getPhonesByStatus(eq(PhoneNumber.PhoneStatus.active), any())).thenReturn(page);

        mockMvc.perform(get("/statistics/phones/status/{status}", "active"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备统计")
    void testGetDeviceStatistics() throws Exception {
        when(statisticsService.getDeviceStatistics()).thenReturn(
            new com.phonebiz.dto.DeviceStatisticsDTO());

        mockMvc.perform(get("/statistics/devices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取离线设备")
    void testGetOfflineDevices() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setDeviceId("device001");
        device.setDeviceName("Test Device");

        when(statisticsService.getOfflineDevices()).thenReturn(List.of(device));

        mockMvc.perform(get("/statistics/devices/offline"))
                .andExpect(status().isOk());
    }
}

