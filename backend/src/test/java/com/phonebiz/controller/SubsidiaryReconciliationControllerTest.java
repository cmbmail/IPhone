package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.SubsidiaryReconciliation;
import com.phonebiz.service.SubsidiaryReconciliationService;

@WebMvcTest(SubsidiaryReconciliationController.class)
@Import(TestSecurityConfig.class)
class SubsidiaryReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubsidiaryReconciliationService reconciliationService;

    private SubsidiaryReconciliation createTestReconciliation() {
        SubsidiaryReconciliation reconciliation = new SubsidiaryReconciliation();
        reconciliation.setId(1L);
        reconciliation.setBillMonth("2024-01");
        reconciliation.setReconciliationStatus(SubsidiaryReconciliation.ReconciliationStatus.pending);
        return reconciliation;
    }

    @Test
    @DisplayName("测试获取对账列表")
    void testGetReconciliations() throws Exception {
        Map<String, Object> reconciliation = new HashMap<>();
        reconciliation.put("id", 1L);
        reconciliation.put("billMonth", "2024-01");
        Page<Map<String, Object>> page = new PageImpl<>(List.of(reconciliation));
        when(reconciliationService.getReconciliationsWithOrgName(anyString(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/reconciliations").param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取对账详情")
    void testGetReconciliation() throws Exception {
        when(reconciliationService.getReconciliation(1L)).thenReturn(createTestReconciliation());

        mockMvc.perform(get("/reconciliations/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取待对账列表")
    void testGetPendingReconciliations() throws Exception {
        when(reconciliationService.getPendingReconciliations(1L)).thenReturn(List.of(createTestReconciliation()));

        mockMvc.perform(get("/reconciliations/pending").param("orgId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试子公司确认")
    void testSubsidiaryConfirm() throws Exception {
        SubsidiaryReconciliation reconciliation = createTestReconciliation();
        reconciliation.setReconciliationStatus(SubsidiaryReconciliation.ReconciliationStatus.confirmed_by_subsidiary);
        when(reconciliationService.subsidiaryConfirm(1L, "admin")).thenReturn(reconciliation);

        mockMvc.perform(post("/reconciliations/{id}/subsidiary-confirm", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试集团确认")
    void testGroupConfirm() throws Exception {
        SubsidiaryReconciliation reconciliation = createTestReconciliation();
        reconciliation.setReconciliationStatus(SubsidiaryReconciliation.ReconciliationStatus.confirmed_by_group);
        when(reconciliationService.groupConfirm(1L, "admin")).thenReturn(reconciliation);

        mockMvc.perform(post("/reconciliations/{id}/group-confirm", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取对账汇总")
    void testGetSummary() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrg", 10);
        summary.put("confirmedOrg", 8);
        when(reconciliationService.getReconciliationSummary("2024-01")).thenReturn(summary);

        mockMvc.perform(get("/reconciliations/summary").param("billMonth", "2024-01"))
                .andExpect(status().isOk());
    }
}

