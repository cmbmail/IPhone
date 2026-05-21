package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.service.CostCenterService;

@WebMvcTest(CostCenterController.class)
@Import(TestSecurityConfig.class)
class CostCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CostCenterService costCenterService;

    private CostCenterMapping createTestMapping() {
        CostCenterMapping mapping = new CostCenterMapping();
        mapping.setId(1L);
        mapping.setOrgId(1L);
        mapping.setCostCenterCode("CC001");
        mapping.setCostCenterName("Test Cost Center");
        mapping.setStatus(CostCenterMapping.CostCenterStatus.active);
        return mapping;
    }

    @Test
    @DisplayName("测试获取所有成本中心")
    void testGetAllCostCenters() throws Exception {
        when(costCenterService.getAllCostCenters()).thenReturn(List.of(createTestMapping()));

        mockMvc.perform(get("/cost-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试获取成本中心详情")
    void testGetCostCenterById() throws Exception {
        when(costCenterService.getCostCenterById(1L)).thenReturn(createTestMapping());

        mockMvc.perform(get("/cost-centers/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取组织下成本中心")
    void testGetCostCentersByOrg() throws Exception {
        when(costCenterService.getCostCentersByOrg(1L)).thenReturn(List.of(createTestMapping()));

        mockMvc.perform(get("/cost-centers/org/{orgId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试创建成本中心")
    void testCreateCostCenter() throws Exception {
        when(costCenterService.createCostCenter(any(), anyString())).thenReturn(createTestMapping());

        com.phonebiz.dto.CreateCostCenterRequest request = new com.phonebiz.dto.CreateCostCenterRequest();
        request.setOrgId(1L);
        request.setCostCenterCode("CC001");
        request.setCostCenterName("New Cost Center");

        mockMvc.perform(post("/cost-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新成本中心")
    void testUpdateCostCenter() throws Exception {
        when(costCenterService.updateCostCenter(eq(1L), any(), anyString())).thenReturn(createTestMapping());

        com.phonebiz.dto.UpdateCostCenterRequest request = new com.phonebiz.dto.UpdateCostCenterRequest();
        request.setCostCenterName("Updated Cost Center");

        mockMvc.perform(put("/cost-centers/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除成本中心")
    void testDeleteCostCenter() throws Exception {
        mockMvc.perform(delete("/cost-centers/{id}", 1L))
                .andExpect(status().isOk());
    }
}

