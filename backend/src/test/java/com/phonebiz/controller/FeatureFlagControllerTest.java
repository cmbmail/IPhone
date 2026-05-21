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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.SysFeatureFlag;
import com.phonebiz.service.FeatureFlagService;

@WebMvcTest(FeatureFlagController.class)
@Import(TestSecurityConfig.class)
class FeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureFlagService featureFlagService;

    @Test
    @DisplayName("测试获取功能开关")
    void testGetFeatureFlag() throws Exception {
        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey("test-feature")
                .featureName("Test Feature")
                .isEnabled(true)
                .build();
        when(featureFlagService.getFeatureFlag("test-feature")).thenReturn(flag);

        mockMvc.perform(get("/feature-flags/{featureKey}", "test-feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureKey").value("test-feature"));
    }

    @Test
    @DisplayName("测试检查功能开关")
    void testCheckFeatureEnabled() throws Exception {
        when(featureFlagService.isFeatureEnabled(anyString(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(get("/feature-flags/test-feature/check")
                        .param("orgId", "1")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("测试创建功能开关")
    void testCreateFeatureFlag() throws Exception {
        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey("new-feature")
                .featureName("New Feature")
                .isEnabled(true)
                .build();
        when(featureFlagService.createFeatureFlag(anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString()))
                .thenReturn(flag);

        FeatureFlagController.CreateFeatureFlagRequest request = new FeatureFlagController.CreateFeatureFlagRequest();
        request.setFeatureKey("new-feature");
        request.setFeatureName("New Feature");
        request.setDescription("Test");
        request.setIsEnabled(true);
        request.setScopeType("GLOBAL");
        request.setScopeValue("*");

        mockMvc.perform(post("/feature-flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureKey").value("new-feature"));
    }

    @Test
    @DisplayName("测试切换功能开关")
    void testToggleFeatureFlag() throws Exception {
        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey("test-feature")
                .isEnabled(false)
                .build();
        when(featureFlagService.updateFeatureFlag("test-feature", false)).thenReturn(flag);

        mockMvc.perform(put("/feature-flags/test-feature/toggle")
                        .param("isEnabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isEnabled").value(false));
    }

    @Test
    @DisplayName("测试更新功能开关范围")
    void testUpdateFeatureFlagScope() throws Exception {
        SysFeatureFlag flag = SysFeatureFlag.builder()
                .featureKey("test-feature")
                .scopeType("ORG")
                .scopeValue("1")
                .build();
        when(featureFlagService.updateFeatureFlagScope("test-feature", "ORG", "1")).thenReturn(flag);

        mockMvc.perform(put("/feature-flags/test-feature/scope")
                        .param("scopeType", "ORG")
                        .param("scopeValue", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("ORG"));
    }
}