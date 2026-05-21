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
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.service.SnapshotService;

@WebMvcTest(SnapshotController.class)
@Import(TestSecurityConfig.class)
class SnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SnapshotService snapshotService;

    @Test
    @DisplayName("测试获取快照列表")
    void testGetSnapshots() throws Exception {
        PhoneSnapshot snapshot = PhoneSnapshot.builder()
                .id(1L)
                .snapshotMonth("2024-01")
                .phoneNumber("13800138000")
                .build();
        when(snapshotService.getSnapshotsByMonth("2024-01")).thenReturn(List.of(snapshot));

        mockMvc.perform(get("/snapshots/month/{month}", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("测试获取可用月份")
    void testGetAvailableMonths() throws Exception {
        when(snapshotService.getAvailableMonths()).thenReturn(List.of("2024-01", "2024-02"));

        mockMvc.perform(get("/snapshots/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("测试触发快照生成")
    void testTriggerSnapshot() throws Exception {
        doNothing().when(snapshotService).triggerSnapshot("2024-01");

        mockMvc.perform(post("/snapshots/trigger").param("month", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试重新生成快照")
    void testRegenerateSnapshot() throws Exception {
        doNothing().when(snapshotService).regenerateSnapshot("2024-01");

        mockMvc.perform(post("/snapshots/{month}/regenerate", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取快照统计")
    void testGetSnapshotCount() throws Exception {
        when(snapshotService.getSnapshotCount("2024-01")).thenReturn(100);
        when(snapshotService.getSnapshotCountByStatus("2024-01", "ACTIVE")).thenReturn(80);
        when(snapshotService.getSnapshotCountByStatus("2024-01", "STOPPED")).thenReturn(15);
        when(snapshotService.getSnapshotCountByStatus("2024-01", "CANCELLED")).thenReturn(5);

        mockMvc.perform(get("/snapshots/{month}/count", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }

    @Test
    @DisplayName("测试按组织获取快照统计")
    void testGetSnapshotCountByOrg() throws Exception {
        when(snapshotService.getSnapshotCountByOrg("2024-01", 1L)).thenReturn(50);

        mockMvc.perform(get("/snapshots/{month}/org/{orgId}/count", "2024-01", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(50));
    }
}

