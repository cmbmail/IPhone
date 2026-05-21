package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.service.BillImportService;

@WebMvcTest(BillController.class)
@Import(TestSecurityConfig.class)
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillImportService billImportService;

    @MockBean
    private BillRawRepository billRawRepository;

    @Test
    @DisplayName("测试导入账单")
    void testImportBill() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test".getBytes()
        );

        when(billImportService.importBillRaw(anyString(), any(), anyString())).thenReturn(10);

        mockMvc.perform(multipart("/bills/import")
                        .file(file)
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试导入并分摊")
    void testImportAndAllocate() throws Exception {
        when(billImportService.importBillRaw(anyString(), any(), anyString())).thenReturn(10);
        doNothing().when(billImportService).processImportAsync(anyString(), anyString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test".getBytes()
        );

        mockMvc.perform(multipart("/bills/import-and-allocate")
                        .file(file)
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }
}

