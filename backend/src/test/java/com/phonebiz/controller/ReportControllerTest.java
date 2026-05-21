package com.phonebiz.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.service.ReportService;

@WebMvcTest(ReportController.class)
@Import(TestSecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("测试获取费用汇总报表")
    void testGetCostSummary() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRecords", 100);

        when(reportService.getBillAllocationReport("2024-01")).thenReturn(summary);

        mockMvc.perform(get("/reports/cost-summary")
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取电话资产报表")
    void testGetPhoneAssetReport() throws Exception {
        Map<String, Object> report = new HashMap<>();
        report.put("totalPhones", 1000);
        report.put("allocatedPhones", 800);

        when(reportService.getPhoneAssetReport("2024-01")).thenReturn(report);

        mockMvc.perform(get("/reports/phone-asset")
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取账单分配报表")
    void testGetBillAllocationReport() throws Exception {
        Map<String, Object> report = new HashMap<>();
        report.put("totalRecords", 100);

        when(reportService.getBillAllocationReport("2024-01")).thenReturn(report);

        mockMvc.perform(get("/reports/bill-allocation")
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取工单报表")
    void testGetWorkOrderReport() throws Exception {
        Map<String, Object> report = new HashMap<>();
        report.put("totalOrders", 50);
        report.put("completedOrders", 40);

        when(reportService.getWorkOrderReport("2024-01-01", "2024-01-31")).thenReturn(report);

        mockMvc.perform(get("/reports/work-order")
                        .param("startTime", "2024-01-01")
                        .param("endTime", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取异常账单报表")
    void testGetAnomalyBillReport() throws Exception {
        Map<String, Object> report = new HashMap<>();
        report.put("totalAnomalies", 10);

        when(reportService.getAnomalyBillReport("2024-01")).thenReturn(report);

        mockMvc.perform(get("/reports/anomaly-bill")
                        .param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }
}

