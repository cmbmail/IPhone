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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.ImportBatch;
import com.phonebiz.service.ImportService;

@WebMvcTest(ImportController.class)
@Import(TestSecurityConfig.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImportService importService;

    @Test
    @DisplayName("测试创建导入批次")
    void testStartPhoneImport() throws Exception {
        ImportBatch batch = ImportBatch.builder()
                .batchId("batch001")
                .status(ImportBatch.ImportStatus.PENDING)
                .build();
        when(importService.createBatch(anyInt(), anyString())).thenReturn(batch);

        mockMvc.perform(post("/import/phone/start")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("batch001"));
    }

    @Test
    @DisplayName("测试上传导入数据")
    void testUploadPhoneData() throws Exception {
        doNothing().when(importService).processImportAsync(anyString(), anyList(), any(), anyString());

        mockMvc.perform(post("/import/phone/{batchId}/upload", "batch001")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(List.of())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取导入状态")
    void testGetImportStatus() throws Exception {
        ImportBatch batch = ImportBatch.builder()
                .batchId("batch001")
                .status(ImportBatch.ImportStatus.PROCESSING)
                .build();
        when(importService.getBatchStatus("batch001")).thenReturn(batch);

        mockMvc.perform(get("/import/phone/{batchId}/status", "batch001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("测试获取导入进度")
    void testGetImportProgress() throws Exception {
        when(importService.getProgress("batch001")).thenReturn(50);

        mockMvc.perform(get("/import/phone/{batchId}/progress", "batch001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progress").value(50));
    }
}

