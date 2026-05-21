package com.phonebiz.controller;

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
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.service.PhoneService;

@WebMvcTest(PhoneController.class)
@Import(TestSecurityConfig.class)
class PhoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhoneService phoneService;

    @Test
    @DisplayName("测试获取号码详情")
    void testGetPhone() throws Exception {
        PhoneNumber phone = new PhoneNumber();
        phone.setId(1L);
        phone.setPhoneNumber("13800138000");
        phone.setStatus(PhoneNumber.PhoneStatus.active);

        when(phoneService.getPhoneById(1L)).thenReturn(phone);

        mockMvc.perform(get("/phones/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取空闲号码")
    void testGetIdlePhones() throws Exception {
        when(phoneService.getIdlePhones()).thenReturn(List.of());

        mockMvc.perform(get("/phones/idle"))
                .andExpect(status().isOk());
    }
}

