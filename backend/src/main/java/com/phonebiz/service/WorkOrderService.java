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
    public Page<WorkOrderDTO> getWorkOrdersByStatus(Integer status, Pageable pageable) {
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
                .type(mapTypeToInteger(request.getType()))
                .status(WorkOrder.WO_PENDING)
                .priority(mapPriorityToInteger(request.getPriority()))
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
                        .itemType(mapItemTypeToInteger(itemRequest.getItemType()))
                        .targetId(itemRequest.getTargetId())
                        .action(itemRequest.getAction())
                        .fromValue(itemRequest.getFromValue())
                        .toValue(itemRequest.getToValue())
                        .status(WorkOrderItem.ITEM_PENDING)
                        .build();
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                workOrderItemRepository.save(item);
            }
        }

        log.info("Work order created: {}", workOrderNo);

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO updateWorkOrder(Long id, UpdateWorkOrderRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (request.getStatus() != null) {
            Integer newStatus = mapStatusToInteger(request.getStatus());
            if (!isValidStatusTransition(workOrder.getStatus(), newStatus)) {
                throw new BusinessException(ErrorCode.WO_002);
            }
            workOrder.setStatus(newStatus);

            if (newStatus == WorkOrder.WO_COMPLETED) {
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

        if (workOrder.getStatus() == WorkOrder.WO_PROCESSING) {
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

    private synchronized String generateWorkOrderNo() {
        String dateStr = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        String prefix = "WO" + dateStr;

        long count = workOrderRepository.countByWorkOrderNoStartingWith(prefix);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        return String.format("%s%04d%s", prefix, count + 1, randomSuffix);
    }

    private boolean isValidStatusTransition(Integer from, Integer to) {
        return switch (from) {
            case WorkOrder.WO_PENDING -> to == WorkOrder.WO_ACCEPTED || to == WorkOrder.WO_CANCELLED;
            case WorkOrder.WO_ACCEPTED -> to == WorkOrder.WO_PROCESSING || to == WorkOrder.WO_CANCELLED;
            case WorkOrder.WO_PROCESSING -> to == WorkOrder.WO_COMPLETED;
            case WorkOrder.WO_COMPLETED, WorkOrder.WO_CANCELLED, WorkOrder.WO_ARCHIVED -> false;
            default -> false;
        };
    }

    private WorkOrderDTO toDTO(WorkOrder workOrder) {
        return WorkOrderDTO.builder()
                .id(workOrder.getId())
                .workOrderNo(workOrder.getWorkOrderNo())
                .type(String.valueOf(workOrder.getType()))
                .status(String.valueOf(workOrder.getStatus()))
                .priority(String.valueOf(workOrder.getPriority()))
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
                .itemType(String.valueOf(item.getItemType()))
                .targetId(item.getTargetId())
                .action(item.getAction())
                .fromValue(item.getFromValue())
                .toValue(item.getToValue())
                .status(String.valueOf(item.getStatus()))
                .executedAt(item.getExecutedAt())
                .errorMessage(item.getErrorMessage())
                .remark(item.getRemark())
                .build();
    }

    @Transactional
    public WorkOrderDTO acceptWorkOrder(Long id, Long handlerId, String handlerName) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WO_PENDING) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WO_ACCEPTED);
        workOrder.setHandlerId(handlerId);
        workOrder.setHandlerName(handlerName);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order accepted: {}", saved.getWorkOrderNo());

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO processWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WO_ACCEPTED) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WO_PROCESSING);
        workOrder.setUpdatedAt(LocalDateTime.now());

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work order processing: {}", saved.getWorkOrderNo());

        return getWorkOrderById(saved.getId());
    }

    @Transactional
    public WorkOrderDTO completeWorkOrder(Long id, String remark) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_001));

        if (workOrder.getStatus() != WorkOrder.WO_PROCESSING) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WO_COMPLETED);
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

        if (workOrder.getStatus() != WorkOrder.WO_PENDING && workOrder.getStatus() != WorkOrder.WO_ACCEPTED) {
            throw new BusinessException(ErrorCode.WO_002);
        }

        workOrder.setStatus(WorkOrder.WO_CANCELLED);
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
                    .status(WorkOrder.WO_PENDING)
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
                        .status(WorkOrderItem.ITEM_PENDING)
                        .build();
                newItem.setCreatedAt(LocalDateTime.now());
                newItem.setUpdatedAt(LocalDateTime.now());
                workOrderItemRepository.save(newItem);
            }

            result.add(getWorkOrderById(savedChild.getId()));
        }

        parentWorkOrder.setStatus(WorkOrder.WO_COMPLETED);
        parentWorkOrder.setCompletedAt(LocalDateTime.now());
        parentWorkOrder.setBatchId(batchId);
        workOrderRepository.save(parentWorkOrder);

        log.info("Batch split work order {} into {} child orders", id, result.size());
        return result;
    }

    @Transactional
    public WorkOrderItemDTO executeWorkOrderItem(Long itemId) {
        WorkOrderItem item = workOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WO_003));

        if (item.getStatus() != WorkOrderItem.ITEM_PENDING) {
            throw new BusinessException(ErrorCode.WO_004);
        }

        item.setStatus(WorkOrderItem.ITEM_PROCESSING);
        item.setUpdatedAt(LocalDateTime.now());
        workOrderItemRepository.save(item);

        try {
            executeItemAction(item);

            item.setStatus(WorkOrderItem.ITEM_COMPLETED);
            item.setExecutedAt(LocalDateTime.now());
        } catch (Exception e) {
            item.setStatus(WorkOrderItem.ITEM_FAILED);
            item.setErrorMessage(e.getMessage());
            log.error("Failed to execute work order item {}: {}", itemId, e.getMessage());
        }

        item.setUpdatedAt(LocalDateTime.now());
        WorkOrderItem saved = workOrderItemRepository.save(item);

        updateParentStatusBasedOnItems(saved.getWorkOrderId());

        return toItemDTO(saved);
    }

    private void executeItemAction(WorkOrderItem item) {
        log.info("Executing work order item: action={}, targetId={}, itemType={}", item.getAction(), item.getTargetId(), item.getItemType());

        if (item.getItemType() == WorkOrderItem.ITEM_PHONE) {
            workOrderDrivenPhoneService.executePhoneWorkOrderItem(item);
        } else if (item.getItemType() == WorkOrderItem.ITEM_DEVICE) {
            workOrderDrivenDeviceService.executeDeviceWorkOrderItem(item);
        } else {
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }

    private void updateParentStatusBasedOnItems(Long workOrderId) {
        long pendingCount = workOrderItemRepository.countByWorkOrderIdAndStatus(workOrderId, WorkOrderItem.ITEM_PENDING);
        long failedCount = workOrderItemRepository.countByWorkOrderIdAndStatus(workOrderId, WorkOrderItem.ITEM_FAILED);

        WorkOrder workOrder = workOrderRepository.findById(workOrderId).orElse(null);
        if (workOrder != null && workOrder.getStatus() == WorkOrder.WO_PROCESSING) {
            if (pendingCount == 0) {
                workOrder.setStatus(WorkOrder.WO_COMPLETED);
                workOrder.setCompletedAt(LocalDateTime.now());
                workOrderRepository.save(workOrder);
            }
        }
    }

    private int mapTypeToInteger(String type) {
        return switch (type.toUpperCase()) {
            case "ALLOCATE" -> WorkOrder.WO_PHONE_ALLOCATE;
            case "TRANSFER" -> WorkOrder.WO_PHONE_TRANSFER;
            case "CHANGE_NUMBER" -> WorkOrder.WO_PHONE_CHANGE_NUMBER;
            case "CHANGE_ORG" -> WorkOrder.WO_PHONE_CHANGE_ORG;
            case "RECLAIM" -> WorkOrder.WO_PHONE_RECLAIM;
            case "SURRENDER" -> WorkOrder.WO_PHONE_SURRENDER;
            case "ENABLE" -> WorkOrder.WO_PHONE_ENABLE;
            case "DISABLE" -> WorkOrder.WO_PHONE_DISABLE;
            default -> throw new BusinessException(ErrorCode.SYS_001);
        };
    }

    private int mapPriorityToInteger(String priority) {
        return switch (priority.toUpperCase()) {
            case "LOW" -> WorkOrder.WO_LOW;
            case "NORMAL" -> WorkOrder.WO_NORMAL;
            case "HIGH" -> WorkOrder.WO_HIGH;
            case "URGENT" -> WorkOrder.WO_URGENT;
            default -> throw new BusinessException(ErrorCode.SYS_001);
        };
    }

    private int mapStatusToInteger(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING" -> WorkOrder.WO_PENDING;
            case "ACCEPTED" -> WorkOrder.WO_ACCEPTED;
            case "PROCESSING" -> WorkOrder.WO_PROCESSING;
            case "COMPLETED" -> WorkOrder.WO_COMPLETED;
            case "ARCHIVED" -> WorkOrder.WO_ARCHIVED;
            case "CANCELLED", "REJECTED" -> WorkOrder.WO_CANCELLED;
            default -> throw new BusinessException(ErrorCode.SYS_001);
        };
    }

    private int mapItemTypeToInteger(String itemType) {
        return switch (itemType.toUpperCase()) {
            case "PHONE" -> WorkOrderItem.ITEM_PHONE;
            case "DEVICE" -> WorkOrderItem.ITEM_DEVICE;
            case "EMPLOYEE" -> WorkOrderItem.ITEM_EMPLOYEE;
            default -> throw new BusinessException(ErrorCode.SYS_001);
        };
    }
}
