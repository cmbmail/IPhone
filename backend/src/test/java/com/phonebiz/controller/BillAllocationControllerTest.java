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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.BillAllocation;
import com.phonebiz.repository.BillAllocationRepository;
import com.phonebiz.service.BillAllocationService;

@WebMvcTest(BillAllocationController.class)
@Import(TestSecurityConfig.class)
class BillAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillAllocationRepository billAllocationRepository;

    @MockBean
    private BillAllocationService billAllocationService;

    @Test
    @DisplayName("测试获取账单分配列表")
    void testGetAllocations() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .billMonth("2024-01")
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonth(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试获取异常账单")
    void testGetAnomalies() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .anomalyFlag(true)
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonthAndAnomalyFlag(anyString(), eq(true), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations/anomalies").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试获取待组织确认账单")
    void testGetPendingOrgConfirm() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .adminConfirmOrg(BillAllocation.ConfirmStatus.pending)
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonthAndAdminConfirmOrg(anyString(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations/pending-org-confirm").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试获取待金额确认账单")
    void testGetPendingAmountConfirm() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .adminConfirmAmount(BillAllocation.ConfirmStatus.pending)
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonthAndAdminConfirmAmount(anyString(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations/pending-amount-confirm").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试获取待财务确认异常账单")
    void testGetPendingFinanceConfirm() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .financeConfirmAnomaly(BillAllocation.FinanceConfirmStatus.pending)
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonthAndFinanceConfirmAnomaly(anyString(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations/pending-finance-confirm").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试获取待提交账单")
    void testGetPendingSubmit() throws Exception {
        BillAllocation allocation = BillAllocation.builder()
                .id(1L)
                .financeConfirmSubmit(BillAllocation.FinanceSubmitStatus.pending)
                .build();
        Page<BillAllocation> page = new PageImpl<>(List.of(allocation));
        when(billAllocationRepository.findByBillMonthAndFinanceConfirmSubmit(anyString(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/bill-allocations/pending-submit").param("billMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("测试组织确认")
    void testConfirmOrg() throws Exception {
        doNothing().when(billAllocationService).adminConfirmOrg(anyLong(), any(), anyString());

        mockMvc.perform(post("/bill-allocations/{id}/confirm-org", 1L)
                        .param("status", "correct"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试金额确认")
    void testConfirmAmount() throws Exception {
        doNothing().when(billAllocationService).adminConfirmAmount(anyLong(), any(), anyString());

        mockMvc.perform(post("/bill-allocations/{id}/confirm-amount", 1L)
                        .param("status", "correct"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试财务确认异常")
    void testConfirmAnomaly() throws Exception {
        doNothing().when(billAllocationService).financeConfirmAnomaly(anyLong(), any(), anyString());

        mockMvc.perform(post("/bill-allocations/{id}/confirm-anomaly", 1L)
                        .param("status", "confirmed"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试提交")
    void testSubmit() throws Exception {
        doNothing().when(billAllocationService).financeSubmit(anyLong(), anyString());

        mockMvc.perform(post("/bill-allocations/{id}/submit", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试驳回")
    void testReject() throws Exception {
        doNothing().when(billAllocationService).rejectAndReset(anyLong(), anyString());

        mockMvc.perform(post("/bill-allocations/{id}/reject", 1L)
                        .param("reason", "Test reason"))
                .andExpect(status().isOk());
    }
}