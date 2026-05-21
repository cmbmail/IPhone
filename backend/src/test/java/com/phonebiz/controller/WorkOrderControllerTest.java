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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.service.WorkOrderService;

@WebMvcTest(WorkOrderController.class)
@Import(TestSecurityConfig.class)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkOrderService workOrderService;

    @Test
    @DisplayName("测试获取工单列表")
    void testGetWorkOrders() throws Exception {
        WorkOrderDTO dto = WorkOrderDTO.builder()
                .id(1L)
                .workOrderNo("WO202401010001")
                .type("PHONE_ALLOCATE")
                .status("PENDING")
                .build();

        Page<WorkOrderDTO> page = new PageImpl<>(List.of(dto));
        when(workOrderService.getWorkOrders(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/work-orders"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取工单详情")
    void testGetWorkOrder() throws Exception {
        WorkOrderDTO dto = WorkOrderDTO.builder()
                .id(1L)
                .workOrderNo("WO202401010001")
                .title("Test Order")
                .build();

        when(workOrderService.getWorkOrderById(1L)).thenReturn(dto);

        mockMvc.perform(get("/work-orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workOrderNo").value("WO202401010001"));
    }

    @Test
    @DisplayName("测试创建工单")
    void testCreateWorkOrder() throws Exception {
        WorkOrderDTO dto = WorkOrderDTO.builder()
                .id(1L)
                .workOrderNo("WO202401010001")
                .title("New Order")
                .build();

        when(workOrderService.createWorkOrder(any(), anyLong(), anyString())).thenReturn(dto);

        com.phonebiz.dto.CreateWorkOrderRequest request = new com.phonebiz.dto.CreateWorkOrderRequest();
        request.setType("PHONE_ALLOCATE");
        request.setTitle("New Order");
        request.setPriority("HIGH");

        mockMvc.perform(post("/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新工单状态")
    void testUpdateWorkOrderStatus() throws Exception {
        WorkOrderDTO dto = WorkOrderDTO.builder()
                .id(1L)
                .status("ACCEPTED")
                .build();
        when(workOrderService.updateWorkOrder(anyLong(), any())).thenReturn(dto);

        com.phonebiz.dto.UpdateWorkOrderRequest request = new com.phonebiz.dto.UpdateWorkOrderRequest();
        request.setStatus("ACCEPTED");

        mockMvc.perform(put("/work-orders/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试删除工单")
    void testDeleteWorkOrder() throws Exception {
        doNothing().when(workOrderService).deleteWorkOrder(1L);

        mockMvc.perform(delete("/work-orders/{id}", 1L))
                .andExpect(status().isOk());
    }
}

