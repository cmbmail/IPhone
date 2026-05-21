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
import com.phonebiz.entity.AreaCodeOrgMapping;
import com.phonebiz.service.AreaCodeService;

@WebMvcTest(AreaCodeController.class)
@Import(TestSecurityConfig.class)
class AreaCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AreaCodeService areaCodeService;

    private AreaCodeOrgMapping createTestMapping() {
        AreaCodeOrgMapping mapping = new AreaCodeOrgMapping();
        mapping.setId(1L);
        mapping.setAreaCode("010");
        mapping.setOrgId(1L);
        mapping.setPriority(1);
        return mapping;
    }

    @Test
    @DisplayName("测试获取所有区号映射")
    void testGetAllMappings() throws Exception {
        when(areaCodeService.getAllMappings()).thenReturn(List.of(createTestMapping()));

        mockMvc.perform(get("/area-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试根据区号获取映射")
    void testGetMappingsByAreaCode() throws Exception {
        when(areaCodeService.getMappingsByAreaCode("010")).thenReturn(List.of(createTestMapping()));

        mockMvc.perform(get("/area-codes/area/{areaCode}", "010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试根据组织获取映射")
    void testGetMappingsByOrg() throws Exception {
        when(areaCodeService.getMappingsByOrg(1L)).thenReturn(List.of(createTestMapping()));

        mockMvc.perform(get("/area-codes/org/{orgId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试创建区号映射")
    void testCreateMapping() throws Exception {
        when(areaCodeService.createMapping(any(), anyString())).thenReturn(createTestMapping());

        com.phonebiz.dto.CreateAreaCodeMappingRequest request = new com.phonebiz.dto.CreateAreaCodeMappingRequest();
        request.setAreaCode("010");
        request.setOrgId(1L);
        request.setPriority(1);

        mockMvc.perform(post("/area-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除区号映射")
    void testDeleteMapping() throws Exception {
        mockMvc.perform(delete("/area-codes/{id}", 1L))
                .andExpect(status().isOk());
    }
}

