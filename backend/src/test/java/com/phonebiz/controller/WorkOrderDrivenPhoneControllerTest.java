package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.service.FeatureFlagService;
import com.phonebiz.service.WorkOrderDrivenPhoneService;

@WebMvcTest(WorkOrderDrivenPhoneController.class)
@Import(TestSecurityConfig.class)
class WorkOrderDrivenPhoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkOrderDrivenPhoneService workOrderDrivenPhoneService;

    @MockBean
    private FeatureFlagService featureFlagService;

    @Test
    @DisplayName("测试获取工单驱动功能开关状态")
    void testIsWorkOrderDrivenEnabled() throws Exception {
        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);

        mockMvc.perform(get("/phone/work-orders/feature/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("测试获取功能开关详情")
    void testGetFeatureFlag() throws Exception {
        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey("test-feature")
                .featureName("Test Feature")
                .isEnabled(true)
                .build();
        when(featureFlagService.getFeatureFlag("test-feature")).thenReturn(flag);

        mockMvc.perform(get("/phone/work-orders/feature/{featureKey}", "test-feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureKey").value("test-feature"));
    }

    @Test
    @DisplayName("测试检查功能开关")
    void testCheckFeatureEnabled() throws Exception {
        when(featureFlagService.isFeatureEnabled(anyString(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(get("/phone/work-orders/feature/test-feature/check")
                        .param("orgId", "1")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("测试创建分配工单")
    void testAllocatePhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.allocatePhoneByWorkOrder(anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/allocate")
                        .param("phoneId", "1")
                        .param("targetOrgId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建归还工单")
    void testSurrenderPhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.surrenderPhoneByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/surrender")
                        .param("phoneId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建调拨工单")
    void testTransferPhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.transferPhoneByWorkOrder(anyLong(), anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/transfer")
                        .param("phoneId", "1")
                        .param("fromOrgId", "1")
                        .param("toOrgId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建改号工单")
    void testChangePhoneNumberByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.changePhoneNumberByWorkOrder(anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/change-number")
                        .param("phoneId", "1")
                        .param("newPhoneNumber", "13800138000"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建转组织工单")
    void testChangePhoneOrgByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.changePhoneOrgByWorkOrder(anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/change-org")
                        .param("phoneId", "1")
                        .param("newOrgId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建回收工单")
    void testReclaimPhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.reclaimPhoneByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/reclaim")
                        .param("phoneId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建启用工单")
    void testEnablePhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.enablePhoneByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/enable")
                        .param("phoneId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建禁用工单")
    void testDisablePhoneByWorkOrder() throws Exception {
        when(workOrderDrivenPhoneService.disablePhoneByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone/work-orders/disable")
                        .param("phoneId", "1"))
                .andExpect(status().isOk());
    }
}