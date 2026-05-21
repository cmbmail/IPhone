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
import com.phonebiz.service.WorkOrderDrivenDeviceService;

@WebMvcTest(WorkOrderDrivenDeviceController.class)
@Import(TestSecurityConfig.class)
class WorkOrderDrivenDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkOrderDrivenDeviceService workOrderDrivenDeviceService;

    @MockBean
    private FeatureFlagService featureFlagService;

    @Test
    @DisplayName("测试获取工单驱动功能开关状态")
    void testIsWorkOrderDrivenEnabled() throws Exception {
        when(featureFlagService.isFeatureEnabled(anyString())).thenReturn(true);

        mockMvc.perform(get("/phone-device/work-orders/feature/enabled"))
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

        mockMvc.perform(get("/phone-device/work-orders/feature/{featureKey}", "test-feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureKey").value("test-feature"));
    }

    @Test
    @DisplayName("测试检查功能开关")
    void testCheckFeatureEnabled() throws Exception {
        when(featureFlagService.isFeatureEnabled(anyString(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(get("/phone-device/work-orders/feature/test-feature/check")
                        .param("orgId", "1")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("测试创建设备分配工单")
    void testAssignDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.assignDeviceByWorkOrder(anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/assign")
                        .param("deviceId", "1")
                        .param("employeeNo", "EMP001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备回收工单")
    void testReclaimDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.reclaimDeviceByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/reclaim")
                        .param("deviceId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备维修工单")
    void testRepairDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.repairDeviceByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/repair")
                        .param("deviceId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备报废工单")
    void testRetireDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.retireDeviceByWorkOrder(anyLong(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/retire")
                        .param("deviceId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备绑号工单")
    void testBindPhoneToDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.bindPhoneToDeviceByWorkOrder(anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/bind-phone")
                        .param("deviceId", "1")
                        .param("extensionNumber", "1001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建设备解绑工单")
    void testUnbindPhoneFromDeviceByWorkOrder() throws Exception {
        when(workOrderDrivenDeviceService.unbindPhoneFromDeviceByWorkOrder(anyLong(), anyString(), anyLong(), anyString()))
                .thenReturn(new WorkOrderDTO());

        mockMvc.perform(post("/phone-device/work-orders/unbind-phone")
                        .param("deviceId", "1")
                        .param("extensionNumber", "1001"))
                .andExpect(status().isOk());
    }
}