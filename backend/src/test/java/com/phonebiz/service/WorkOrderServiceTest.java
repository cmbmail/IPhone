package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.CreateWorkOrderRequest;
import com.phonebiz.dto.UpdateWorkOrderRequest;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.repository.WorkOrderItemRepository;
import com.phonebiz.repository.WorkOrderRepository;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderItemRepository workOrderItemRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        workOrder = WorkOrder.builder()
                .workOrderNo("WO20260515000001")
                .type(WorkOrder.WorkOrderType.PHONE_ALLOCATE)
                .status(WorkOrder.WorkOrderStatus.PENDING)
                .priority(WorkOrder.WorkOrderPriority.MEDIUM)
                .requesterId(1L)
                .requesterName("Test User")
                .title("Test Work Order")
                .description("Test Description")
                .build();
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getWorkOrders_ShouldReturnWorkOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkOrder> page = new PageImpl<>(List.of(workOrder));
        when(workOrderRepository.findAll(pageable)).thenReturn(page);

        Page<WorkOrderDTO> result = workOrderService.getWorkOrders(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("WO20260515000001", result.getContent().get(0).getWorkOrderNo());
    }

    @Test
    void getWorkOrderById_ShouldReturnWorkOrder() {
        workOrder.setId(1L);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        when(workOrderItemRepository.findByWorkOrderId(1L)).thenReturn(new ArrayList<>());

        WorkOrderDTO result = workOrderService.getWorkOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Work Order", result.getTitle());
    }

    @Test
    void getWorkOrderById_ShouldThrowException_WhenNotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> workOrderService.getWorkOrderById(999L));
    }

    @Test
    void createWorkOrder_ShouldCreateNewWorkOrder() {
        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_ALLOCATE")
                .title("New Work Order")
                .description("New Description")
                .priority("HIGH")
                .build();

        when(workOrderRepository.countByWorkOrderNoStartingWith(anyString())).thenReturn(0L);
        
        WorkOrder savedWorkOrder = WorkOrder.builder()
                .workOrderNo("WO20260515000001")
                .type(WorkOrder.WorkOrderType.PHONE_ALLOCATE)
                .status(WorkOrder.WorkOrderStatus.PENDING)
                .priority(WorkOrder.WorkOrderPriority.HIGH)
                .title("New Work Order")
                .build();
        savedWorkOrder.setId(1L);
        savedWorkOrder.setCreatedAt(LocalDateTime.now());
        savedWorkOrder.setUpdatedAt(LocalDateTime.now());
        
        when(workOrderRepository.save(any(WorkOrder.class))).thenReturn(savedWorkOrder);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(savedWorkOrder));
        when(workOrderItemRepository.findByWorkOrderId(1L)).thenReturn(new ArrayList<>());

        WorkOrderDTO result = workOrderService.createWorkOrder(request, 1L, "admin");

        assertNotNull(result);
        assertEquals("New Work Order", result.getTitle());
        assertEquals("PHONE_ALLOCATE", result.getType());
        assertEquals("PENDING", result.getStatus());
        verify(workOrderRepository, times(1)).save(any(WorkOrder.class));
    }

    @Test
    void updateWorkOrder_ShouldUpdateStatus() {
        workOrder.setId(1L);
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .status("ACCEPTED")
                .build();

        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workOrderItemRepository.findByWorkOrderId(1L)).thenReturn(new ArrayList<>());

        WorkOrderDTO result = workOrderService.updateWorkOrder(1L, request);

        assertEquals("ACCEPTED", result.getStatus());
    }

    @Test
    void updateWorkOrder_ShouldThrowException_ForInvalidTransition() {
        workOrder.setId(1L);
        workOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .status("PENDING")
                .build();

        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));

        assertThrows(BusinessException.class, () -> workOrderService.updateWorkOrder(1L, request));
    }

    @Test
    void deleteWorkOrder_ShouldDeleteWorkOrder() {
        workOrder.setId(1L);
        workOrder.setStatus(WorkOrder.WorkOrderStatus.PENDING);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        when(workOrderItemRepository.findByWorkOrderId(1L)).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> workOrderService.deleteWorkOrder(1L));
        verify(workOrderRepository, times(1)).delete(workOrder);
    }

    @Test
    void deleteWorkOrder_ShouldThrowException_WhenProcessing() {
        workOrder.setId(1L);
        workOrder.setStatus(WorkOrder.WorkOrderStatus.PROCESSING);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));

        assertThrows(BusinessException.class, () -> workOrderService.deleteWorkOrder(1L));
    }

    @Test
    void getWorkOrdersByStatus_ShouldReturnFilteredWorkOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkOrder> page = new PageImpl<>(List.of(workOrder));
        when(workOrderRepository.findByStatus(WorkOrder.WorkOrderStatus.PENDING, pageable)).thenReturn(page);

        Page<WorkOrderDTO> result = workOrderService.getWorkOrdersByStatus(WorkOrder.WorkOrderStatus.PENDING, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateWorkOrder_ShouldThrowException_WhenNotFound() {
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .status("ACCEPTED")
                .build();

        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> workOrderService.updateWorkOrder(999L, request));
    }

    @Test
    void deleteWorkOrder_ShouldThrowException_WhenNotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> workOrderService.deleteWorkOrder(999L));
    }

    @Test
    void getWorkOrdersByRequester_ShouldReturnWorkOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkOrder> page = new PageImpl<>(List.of(workOrder));
        when(workOrderRepository.findByRequesterId(1L, pageable)).thenReturn(page);

        Page<WorkOrderDTO> result = workOrderService.getWorkOrdersByRequester(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}

