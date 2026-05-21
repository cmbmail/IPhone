package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.entity.ImportBatch;
import com.phonebiz.repository.ImportBatchRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private ImportBatchRepository batchRepository;

    @Mock
    private PhoneNumberRepository phoneRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ImportService importService;

    private ImportBatch batch;

    @BeforeEach
    void setUp() {
        batch = ImportBatch.builder()
                .batchId("test-batch-id")
                .totalCount(10)
                .successCount(0)
                .failCount(0)
                .status(ImportBatch.ImportStatus.PENDING)
                .operator("admin")
                .build();
    }

    @Test
    @DisplayName("测试创建导入批次")
    void testCreateBatch() {
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportBatch result = importService.createBatch(100, "admin");

        assertNotNull(result);
        assertNotNull(result.getBatchId());
        assertEquals(100, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(ImportBatch.ImportStatus.PENDING, result.getStatus());
        verify(batchRepository, times(1)).save(any(ImportBatch.class));
    }

    @Test
    @DisplayName("测试获取批次状态 - 成功")
    void testGetBatchStatus_Success() {
        when(batchRepository.findByBatchId("test-batch-id")).thenReturn(Optional.of(batch));

        ImportBatch result = importService.getBatchStatus("test-batch-id");

        assertNotNull(result);
        assertEquals("test-batch-id", result.getBatchId());
    }

    @Test
    @DisplayName("测试获取批次状态 - 批次不存在")
    void testGetBatchStatus_NotFound() {
        when(batchRepository.findByBatchId("not-exist")).thenReturn(Optional.empty());

        assertThrows(Exception.class,
            () -> importService.getBatchStatus("not-exist"));
    }

    @Test
    @DisplayName("测试获取导入进度")
    void testGetProgress() {
        assertEquals(0, importService.getProgress("test-batch-id"));
    }

    @Test
    @DisplayName("测试处理导入 - 空数据")
    void testProcessImport_EmptyData() {
        when(batchRepository.findByBatchId("test-batch-id")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<java.util.Map<String, Object>> dataList = new ArrayList<>();

        importService.processImport("test-batch-id", dataList,
            com.phonebiz.dto.PhoneImportRequest.ConflictStrategy.ERROR, "admin");

        verify(batchRepository, atLeastOnce()).save(any(ImportBatch.class));
    }
}

