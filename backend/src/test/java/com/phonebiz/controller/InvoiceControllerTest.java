package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.Invoice;
import com.phonebiz.service.InvoiceService;

@WebMvcTest(InvoiceController.class)
@Import(TestSecurityConfig.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    @DisplayName("测试上传发票")
    void testUploadInvoice() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test content".getBytes());
        Invoice invoice = Invoice.builder()
                .id(1L)
                .billMonth("2024-01")
                .status(Invoice.InvoiceStatus.pending)
                .build();
        when(invoiceService.uploadInvoice(any(), anyString(), anyLong(), anyString())).thenReturn(invoice);

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .param("billMonth", "2024-01")
                        .param("sourceOrgId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取发票列表")
    void testGetInvoices() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .billMonth("2024-01")
                .status(Invoice.InvoiceStatus.pending)
                .build();
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceService.getInvoicesByOrg(any(), any())).thenReturn(page);

        mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取发票详情")
    void testGetInvoice() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .billMonth("2024-01")
                .build();
        when(invoiceService.getInvoiceById(1L)).thenReturn(invoice);

        mockMvc.perform(get("/invoices/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试确认发票")
    void testConfirmInvoice() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .status(Invoice.InvoiceStatus.confirmed)
                .build();
        when(invoiceService.confirmInvoice(anyLong(), anyString())).thenReturn(invoice);

        mockMvc.perform(post("/invoices/{id}/confirm", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试标记已读")
    void testMarkAsRead() throws Exception {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .readAt(java.time.LocalDateTime.now())
                .build();
        when(invoiceService.markAsRead(1L)).thenReturn(invoice);

        mockMvc.perform(post("/invoices/{id}/read", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除发票")
    void testDeleteInvoice() throws Exception {
        doNothing().when(invoiceService).deleteInvoice(1L);

        mockMvc.perform(delete("/invoices/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取发票统计")
    void testGetStatistics() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", 10L);
        stats.put("pending", 5L);
        when(invoiceService.getInvoiceStatistics("2024-01")).thenReturn(stats);

        mockMvc.perform(get("/invoices/statistics").param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }
}

