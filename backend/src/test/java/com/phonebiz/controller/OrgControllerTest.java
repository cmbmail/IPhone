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
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.service.OrgService;

@WebMvcTest(OrgController.class)
@Import(TestSecurityConfig.class)
class OrgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrgService orgService;

    private OrgStructure createTestOrg() {
        OrgStructure org = new OrgStructure();
        org.setId(1L);
        org.setName("Test Org");
        org.setPath("/1");
        org.setStatus(OrgStructure.OrgStatus.active);
        org.setType(OrgStructure.OrgType.dept);
        org.setLevel(0);
        return org;
    }

    @Test
    @DisplayName("测试获取组织树")
    void testGetOrgTree() throws Exception {
        when(orgService.getOrgTree()).thenReturn(List.of(createTestOrg()));

        mockMvc.perform(get("/orgs/tree"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取所有组织")
    void testGetAllOrgs() throws Exception {
        when(orgService.getAllActiveOrgs()).thenReturn(List.of(createTestOrg()));

        mockMvc.perform(get("/orgs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取组织详情")
    void testGetOrgById() throws Exception {
        when(orgService.getOrgById(1L)).thenReturn(createTestOrg());

        mockMvc.perform(get("/orgs/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取子组织")
    void testGetChildren() throws Exception {
        when(orgService.getChildren(1L)).thenReturn(List.of());

        mockMvc.perform(get("/orgs/{id}/children", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建组织")
    void testCreateOrg() throws Exception {
        when(orgService.createOrg(any(), anyString())).thenReturn(createTestOrg());

        mockMvc.perform(post("/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Org\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新组织")
    void testUpdateOrg() throws Exception {
        when(orgService.updateOrg(eq(1L), any(), anyString())).thenReturn(createTestOrg());

        mockMvc.perform(put("/orgs/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Org\"}"))
                .andExpect(status().isOk());
    }
}

