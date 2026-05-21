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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.dto.PhoneDeviceDTO;
import com.phonebiz.service.PhoneDeviceService;

@WebMvcTest(PhoneDeviceController.class)
@Import(TestSecurityConfig.class)
class PhoneDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhoneDeviceService phoneDeviceService;

    @Test
    @DisplayName("测试获取设备列表")
    void testGetDevices() throws Exception {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(1L);
        dto.setMacAddress("001122334455");

        Page<PhoneDeviceDTO> page = new PageImpl<>(List.of(dto));
        when(phoneDeviceService.getDeviceList(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/phone-devices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备详情")
    void testGetDevice() throws Exception {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(1L);
        dto.setMacAddress("001122334455");

        when(phoneDeviceService.getDeviceDetail(1L)).thenReturn(dto);

        mockMvc.perform(get("/phone-devices/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备")
    void testCreateDevice() throws Exception {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(1L);
        dto.setMacAddress("001122334455");

        when(phoneDeviceService.createDevice(any())).thenReturn(dto);

        mockMvc.perform(post("/phone-devices")
                        .contentType("application/json")
                        .content("{\"macAddress\":\"001122334455\",\"model\":\"SIP-T46G\",\"brand\":\"Yealink\",\"orgId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新设备")
    void testUpdateDevice() throws Exception {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(1L);
        dto.setMacAddress("001122334455");

        when(phoneDeviceService.updateDevice(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/phone-devices/{id}", 1L)
                        .contentType("application/json")
                        .content("{\"model\":\"SIP-T48G\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备绑定的号码")
    void testGetBoundPhones() throws Exception {
        when(phoneDeviceService.getBoundPhones(1L)).thenReturn(List.of());

        mockMvc.perform(get("/phone-devices/{id}/phones", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备历史")
    void testGetDeviceHistory() throws Exception {
        when(phoneDeviceService.getDeviceHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/phone-devices/{id}/history", 1L))
                .andExpect(status().isOk());
    }
}

