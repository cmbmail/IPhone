package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.repository.PhoneSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class BillImportServiceTest {

    @Mock
    private BillRawRepository billRawRepository;

    @Mock
    private PhoneSnapshotRepository phoneSnapshotRepository;

    @Mock
    private BillAllocationService billAllocationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BillImportService billImportService;

    @Test
    @DisplayName("测试导入账单 - 无效文件格式")
    void testImportBillRaw_InvalidFile() {
        MultipartFile invalidFile = new MockMultipartFile("test.txt", "test.txt",
                "text/plain", "test content".getBytes());

        assertThrows(Exception.class, () ->
            billImportService.importBillRaw("2024-01", invalidFile, "admin"));
    }

    @Test
    @DisplayName("测试异步处理导入")
    void testProcessImportAsync() {
        assertDoesNotThrow(() -> billImportService.processImportAsync("2024-01", "admin"));
    }

    @Test
    @DisplayName("测试处理并分摊")
    void testProcessAndAllocate_Empty() {
        when(billRawRepository.findByBillMonthAndImportStatus(anyString(), any())).thenReturn(List.of());

        assertDoesNotThrow(() -> billImportService.processAndAllocate("2024-01", "admin"));
    }
}

