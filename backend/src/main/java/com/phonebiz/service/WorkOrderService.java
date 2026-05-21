package com.phonebiz.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateWorkOrderRequest;
import com.phonebiz.dto.UpdateWorkOrderRequest;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.dto.WorkOrderItemDTO;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.repository.WorkOrderItemRepository;
import com.phonebiz.repository.WorkOrderRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository workOrderItemRepository;
    private final WorkOrderDrivenPhoneService workOrderDrivenPhoneService;
    private final WorkOrderDrivenDeviceService workOrderDrivenDeviceService;

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public Page<WorkOrderDTO> getWorkOrders(Pageable pageable) {
        return workOrderRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderDTO> getWorkOrdersByStatus(WorkOrder.WorkOrderStatus status, Pageable pageable) {
        return workOrderRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderDTO> getWorkOrdersByRequester(Long requesterId, Pageable pageable) {
        return workOrderRepository.findByRequesterId(requesterId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderDTO> getWorkOrdersByHandler(Long handlerId, Pageable pageable) {
        return workOrderRepository.findByHandlerId(handlerId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public WorkOrderDTO getWorkOrderById(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));
        
        WorkOrderDTO dto = toDTO(workOrder);
        dto.setItems(getWorkOrderItems(id));
        return dto;
    }

    @Transactional(readOnly = true)
    public WorkOrderDTO getWorkOrderByNo(String workOrderNo) {
        WorkOrder workOrder = workOrderRepository.findByWorkOrderNo(workOrderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));
        
        WorkOrderDTO dto = toDTO(workOrder);
        dto.setItems(getWorkOrderItems(workOrder.getId()));
        return dto;
    }

    @Transactional
    public WorkOrderDTO createWorkOrder(CreateWorkOrderRequest request, Long requesterId, String requesterName) {
        String workOrderNo = generateWorkOrderNo();
        
        WorkOrder workOrder = WorkOrder.builder()
                .workOrderNo(workOrderNo)
                .type(WorkOrder.WorkOrderType.valueOf(request.getType().toUpperCase()))
                .status(WorkOrder.WorkOrderStatus.PENDING)
                .priority(WorkOrder.WorkOrderPriority.valueOf(request.getPriority().toUpperCase()))
                .requesterId(requesterId)
                .requesterName(requesterName)
                .handlerId(request.getHandlerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
        
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(workOrder);
        
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CreateWorkOrderRequest.WorkOrderItemRequest itemRequest : request.getItems()) {
                WorkOrderItem item = WorkOrderItem.builder()
                        .workOrderId(saved.getId())
                        .itemType(WorkOrderItem.ItemType.valueOf(itemRequest.getItemType().toUpperCase()))
                        .targetId(itemRequest.getTargetId())
                        .action(itemRequest.getAction())
                        .fromValue(itemRequest.getFromValue())
                        .toValue(itemRequest.getToValue())
                        .status(WorkOrderItem.ItemStatus.PENDING)
                        .build();
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                workOrderItemRepository.save(item);
            }
        }
        
        log.info("Work order created: {} by {}", workOrderNo, requesterName);
        
        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO updateWorkOrder(Long id, UpdateWorkOrderRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));
        
        if (request.getStatus() != null) {
            WorkOrder.WorkOrderStatus newStatus = WorkOrder.WorkOrderStatus.valueOf(request.getStatus().toUpperCase());
            if (!isValidStatusTransition(workOrder.getStatus(), newStatus)) {
                throw new BusinessException(ErrorCode.WO_002);
            }
            workOrder.setStatus(newStatus);
            
            if (newStatus == WorkOrder.WorkOrderStatus.COMPLETED) {
                workOrder.setCompletedAt(LocalDateTime.now());
            }
        }
        
        if (request.getHandlerId() != null) {
            workOrder.setHandlerId(request.getHandlerId());
        }
        
        if (request.getDescription() != null) {
            workOrder.setDescription(request.getDescription());
        }
        
        if (request.getRemark() != null) {
            workOrder.setRemark(request.getRemark());
        }
        
        workOrder.setUpdatedAt(LocalDateTime.now());
        
        WorkOrder saved = workOrderRepository.save(workOrder);
        
        log.info("Work order updated: {}", saved.getWorkOrderNo());
        
        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public void deleteWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));
        
        if (workOrder.getStatus() == WorkOrder.WorkOrderStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.WO_002);
        }
        
        workOrderItemRepository.findByWorkOrderId(id).forEach(workOrderItemRepository::delete);
        workOrderRepository.delete(workOrder);
        
        log.info("Work order deleted: {}", workOrder.getWorkOrderNo());
    }

    private List<WorkOrderItemDTO> getWorkOrderItems(Long workOrderId) {
        return workOrderItemRepository.findByWorkOrderId(workOrderId).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    private String generateWorkOrderNo() {
        String dateStr = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        String prefix = "WO" + dateStr;
        
        long count = workOrderRepository.countByWorkOrderNoStartingWith(prefix);
        
        return String.format("%s%06d", prefix, count + 1);
    }

    private boolean isValidStatusTransition(WorkOrder.WorkOrderStatus from, WorkOrder.WorkOrderStatus to) {
        return switch (from) {
            case PENDING -> to == WorkOrder.WorkOrderStatus.ACCEPTED || to == WorkOrder.WorkOrderStatus.REJECTED;
            case ACCEPTED -> to == WorkOrder.WorkOrderStatus.PROCESSING || to == WorkOrder.WorkOrderStatus.REJECTED;
            case PROCESSING -> to == WorkOrder.WorkOrderStatus.COMPLETED;
            case COMPLETED, REJECTED, ARCHIVED -> false;
        };
    }

    private WorkOrderDTO toDTO(WorkOrder workOrder) {
        return WorkOrderDTO.builder()
                .id(workOrder.getId())
                .workOrderNo(workOrder.getWorkOrderNo())
                .type(workOrder.getType().name())
                .status(workOrder.getStatus().name())
                .priority(workOrder.getPriority().name())
                .requesterId(workOrder.getRequesterId())
                .requesterName(workOrder.getRequesterName())
                .handlerId(workOrder.getHandlerId())
                .handlerName(workOrder.getHandlerName())
                .batchId(workOrder.getBatchId())
                .title(workOrder.getTitle())
                .description(workOrder.getDescription())
                .completedAt(workOrder.getCompletedAt())
                .remark(workOrder.getRemark())
                .createdAt(workOrder.getCreatedAt())
                .updatedAt(workOrder.getUpdatedAt())
                .build();
    }

    private WorkOrderItemDTO toItemDTO(WorkOrderItem item) {
        return WorkOrderItemDTO.builder()
                .id(item.getId())
                .workOrderId(item.getWorkOrderId())
                .itemType(item.getItemType().name())
                .targetId(item.getTargetId())
                .action(item.getAction())
                .fromValue(item.getFromValue())
                .toValue(item.getToValue())
                .status(item.getStatus().name())
                .executedAt(item.getExecutedAt())
                .errorMessage(item.getErrorMessage())
                .remark(item.getRemark())
                .build();
    }

    @Transactional
    public WorkOrderDTO acceptWorkOrder(Long id, Long handlerId, String handlerName) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WorkOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WorkOrderStatus.ACCEPTED);
        workOrder.setHandlerId(handlerId);
        workOrder.setHandlerName(handlerName);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order accepted: {} by {}", saved.getWorkOrderNo(), handlerName);

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO processWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WorkOrderStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WorkOrderStatus.PROCESSING);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order processing: {}", saved.getWorkOrderNo());

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO completeWorkOrder(Long id, String remark) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WorkOrderStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
        workOrder.setCompletedAt(LocalDateTime.now());
        workOrder.setRemark(remark);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order completed: {}", saved.getWorkOrderNo());

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO rejectWorkOrder(Long id, String reason) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WorkOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WorkOrderStatus.REJECTED);
        workOrder.setRemark(reason);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order rejected: {}", saved.getWorkOrderNo());

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public List<WorkOrderDTO> batchSplitWorkOrder(Long id) {
        WorkOrder parentWorkOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        List<WorkOrderItem> items = workOrderItemRepository.findByWorkOrderId(id);
        
        Map<Long, List<WorkOrderItem>> itemsByOrg = items.stream()
                .filter(item -> item.getTargetId() != null)
                .collect(Collectors.groupingBy(WorkOrderItem::getTargetId));

        String batchId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        List<WorkOrderDTO> result = new ArrayList<>();

        for (Map.Entry<Long, List<WorkOrderItem>> entry : itemsByOrg.entrySet()) {
            WorkOrder childWorkOrder = WorkOrder.builder()
                    .workOrderNo(generateWorkOrderNo())
                    .type(parentWorkOrder.getType())
                    .status(WorkOrder.WorkOrderStatus.PENDING)
                    .priority(parentWorkOrder.getPriority())
                    .requesterId(parentWorkOrder.getRequesterId())
                    .requesterName(parentWorkOrder.getRequesterName())
                    .batchId(batchId)
                    .title(parentWorkOrder.getTitle())
                    .description(parentWorkOrder.getDescription())
                    .build();
            childWorkOrder.setCreatedAt(LocalDateTime.now());
            childWorkOrder.setUpdatedAt(LocalDateTime.now());

            WorkOrder savedChild = workOrderRepository.save(childWorkOrder);

            for (WorkOrderItem item : entry.getValue()) {
                WorkOrderItem newItem = WorkOrderItem.builder()
                        .workOrderId(savedChild.getId())
                        .itemType(item.getItemType())
                        .targetId(item.getTargetId())
                        .action(item.getAction())
                        .fromValue(item.getFromValue())
                        .toValue(item.getToValue())
                        .status(WorkOrderItem.ItemStatus.PENDING)
                        .build();
                newItem.setCreatedAt(LocalDateTime.now());
                newItem.setUpdatedAt(LocalDateTime.now());
                workOrderItemRepository.save(newItem);
            }

            parentWorkOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
            parentWorkOrder.setCompletedAt(LocalDateTime.now());
            parentWorkOrder.setBatchId(batchId);
            workOrderRepository.save(parentWorkOrder);

            result.add(getWorkOrderById(savedChild.getId()));
        }

        log.info("Batch split work order {} into {} child orders", id, result.size());
        return result;
    }

    @Transactional
    public WorkOrderItemDTO executeWorkOrderItem(Long itemId) {
        WorkOrderItem item = workOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_003));

        if (item.getStatus() != WorkOrderItem.ItemStatus.PENDING) {
            throw new BusinessException(ErrorCode.WO_004);
        }

        item.setStatus(WorkOrderItem.ItemStatus.PROCESSING);
        item.setUpdatedAt(LocalDateTime.now());
        workOrderItemRepository.save(item);

        try {
            executeItemAction(item);
            
            item.setStatus(WorkOrderItem.ItemStatus.COMPLETED);
            item.setExecutedAt(LocalDateTime.now());
        } catch (Exception e) {
            item.setStatus(WorkOrderItem.ItemStatus.FAILED);
            item.setErrorMessage(e.getMessage());
            log.error("Failed to execute work order item {}: {}", itemId, e.getMessage());
        }

        item.setUpdatedAt(LocalDateTime.now());
        WorkOrderItem saved = workOrderItemRepository.save(item);

        updateWorkOrderStatusBasedOnItems(saved.getWorkOrderId());

        return toItemDTO(saved);
    }

    private void executeItemAction(WorkOrderItem item) {
        log.info("Executing work order item: action={}, targetId={}, itemType={}", item.getAction(), item.getTargetId(), item.getItemType());

        if (item.getItemType() == WorkOrderItem.ItemType.PHONE) {
            workOrderDrivenPhoneService.executePhoneWorkOrderItem(item);
        } else if (item.getItemType() == WorkOrderItem.ItemType.DEVICE) {
            workOrderDrivenDeviceService.executeDeviceWorkOrderItem(item);
        } else {
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }

    private void updateWorkOrderStatusBasedOnItems(Long workOrderId) {
        long pendingCount = workOrderItemRepository.countByWorkOrderIdAndStatus(workOrderId, WorkOrderItem.ItemStatus.PENDING);
        long failedCount = workOrderItemRepository.countByWorkOrderIdAndStatus(workOrderId, WorkOrderItem.ItemStatus.FAILED);

        WorkOrder workOrder = workOrderRepository.findById(workOrderId).orElse(null);
        if (workOrder != null && workOrder.getStatus() == WorkOrder.WorkOrderStatus.PROCESSING) {
            if (failedCount > 0 && pendingCount == 0) {
                workOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
                workOrder.setCompletedAt(LocalDateTime.now());
                workOrderRepository.save(workOrder);
            } else if (pendingCount == 0) {
                workOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
                workOrder.setCompletedAt(LocalDateTime.now());
                workOrderRepository.save(workOrder);
            }
        }
    }
}

