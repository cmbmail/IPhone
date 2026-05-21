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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.service.ExtensionPoolService;

@WebMvcTest(ExtensionPoolController.class)
@Import(TestSecurityConfig.class)
class ExtensionPoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExtensionPoolService extensionPoolService;

    private ExtensionPool createTestPool() {
        ExtensionPool pool = new ExtensionPool();
        pool.setId(1L);
        pool.setOrgId(1L);
        pool.setStartNumber("1000");
        pool.setEndNumber("1999");
        pool.setAllocatedBy("admin");
        return pool;
    }

    @Test
    @DisplayName("测试获取所有分机号池")
    void testGetAllPools() throws Exception {
        when(extensionPoolService.getAllPools()).thenReturn(List.of(createTestPool()));

        mockMvc.perform(get("/extension-pools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试获取分机号池详情")
    void testGetPoolById() throws Exception {
        when(extensionPoolService.getPoolById(1L)).thenReturn(createTestPool());

        mockMvc.perform(get("/extension-pools/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建分机号池")
    void testCreatePool() throws Exception {
        when(extensionPoolService.createPool(any(), anyString())).thenReturn(createTestPool());

        com.phonebiz.dto.CreateExtensionPoolRequest request = new com.phonebiz.dto.CreateExtensionPoolRequest();
        request.setOrgId(1L);
        request.setStartNumber("1000");
        request.setEndNumber("1999");

        mockMvc.perform(post("/extension-pools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除分机号池")
    void testDeletePool() throws Exception {
        mockMvc.perform(delete("/extension-pools/{id}", 1L))
                .andExpect(status().isOk());
    }
}

