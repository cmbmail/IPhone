package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phonebiz.entity.BillAllocation;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.repository.BillAllocationRepository;
import com.phonebiz.repository.BillRawRepository;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.repository.PhoneSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class BillAllocationServiceTest {

    @Mock
    private BillAllocationRepository billAllocationRepository;

    @Mock
    private BillRawRepository billRawRepository;

    @Mock
    private PhoneSnapshotRepository phoneSnapshotRepository;

    @Mock
    private CostCenterMappingRepository costCenterMappingRepository;

    @InjectMocks
    private BillAllocationService billAllocationService;

    private BillRaw billRaw;
    private PhoneSnapshot snapshot;

    @BeforeEach
    void setUp() {
        billRaw = BillRaw.builder()
                .id(1L)
                .billMonth("2024-01")
                .phoneNumber("13800138000")
                .chargeAmount(new BigDecimal("100.00"))
                .build();

        snapshot = PhoneSnapshot.builder()
                .phoneNumber("13800138000")
                .phoneId(1L)
                .orgId(1L)
                .orgName("Test Org")
                .costCenterCode("CC001")
                .build();
    }

    @Test
    @DisplayName("测试自动分摊 - 成功")
    void testAutoAllocateAndSave_Success() {
        when(phoneSnapshotRepository.findBySnapshotMonth("2024-01")).thenReturn(List.of(snapshot));
        when(billAllocationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        billAllocationService.autoAllocateAndSave(List.of(billRaw), "2024-01", "admin");

        verify(billAllocationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("测试自动分摊 - 无快照匹配")
    void testAutoAllocateAndSave_NoSnapshot() {
        when(phoneSnapshotRepository.findBySnapshotMonth("2024-01")).thenReturn(List.of());
        when(billAllocationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillAllocation> allocations = invocation.getArgument(0);
            assertTrue(allocations.get(0).getAnomalyFlag());
            return allocations;
        });

        billAllocationService.autoAllocateAndSave(List.of(billRaw), "2024-01", "admin");

        verify(billAllocationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("测试自动分摊 - 空账单列表")
    void testAutoAllocateAndSave_EmptyList() {
        when(billAllocationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        billAllocationService.autoAllocateAndSave(List.of(), "2024-01", "admin");

        verify(billAllocationRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("测试管理员确认组织")
    void testAdminConfirmOrg() {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        when(billAllocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(billAllocationRepository.save(any(BillAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> billAllocationService.adminConfirmOrg(1L, BillAllocation.ConfirmStatus.correct, "admin"));
        verify(billAllocationRepository).save(any(BillAllocation.class));
    }

    @Test
    @DisplayName("测试管理员确认金额")
    void testAdminConfirmAmount() {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        when(billAllocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(billAllocationRepository.save(any(BillAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> billAllocationService.adminConfirmAmount(1L, BillAllocation.ConfirmStatus.correct, "admin"));
        verify(billAllocationRepository).save(any(BillAllocation.class));
    }

    @Test
    @DisplayName("测试财务确认异常")
    void testFinanceConfirmAnomaly() {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        when(billAllocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(billAllocationRepository.save(any(BillAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> billAllocationService.financeConfirmAnomaly(1L, BillAllocation.FinanceConfirmStatus.confirmed, "admin"));
        verify(billAllocationRepository).save(any(BillAllocation.class));
    }

    @Test
    @DisplayName("测试财务提交")
    void testFinanceSubmit() {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        when(billAllocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(billAllocationRepository.save(any(BillAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> billAllocationService.financeSubmit(1L, "admin"));
        verify(billAllocationRepository).save(any(BillAllocation.class));
    }

    @Test
    @DisplayName("测试驳回并重置")
    void testRejectAndReset() {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();

        when(billAllocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(billAllocationRepository.save(any(BillAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> billAllocationService.rejectAndReset(1L, "Test reason"));
        verify(billAllocationRepository).save(any(BillAllocation.class));
    }
}

