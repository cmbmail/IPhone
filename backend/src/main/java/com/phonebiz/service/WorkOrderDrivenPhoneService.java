package com.phonebiz.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateWorkOrderRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.repository.PhoneNumberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderDrivenPhoneService {

    private final FeatureFlagService featureFlagService;
    private final ApplicationContext applicationContext;
    private final PhoneService phoneService;
    private final PhoneNumberRepository phoneRepository;
    
    private WorkOrderService getWorkOrderService() {
        return applicationContext.getBean(WorkOrderService.class);
    }

    @Transactional
    public WorkOrderDTO allocatePhoneByWorkOrder(Long phoneId, Long targetOrgId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("allocate")
                .fromValue(null)
                .toValue(targetOrgId.toString())
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_ALLOCATE")
                .title("号码分配工单 - " + phone.getPhoneNumber())
                .description("将号码 " + phone.getPhoneNumber() + " 分配到组织 " + targetOrgId)
                .priority("MEDIUM")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone allocation work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO surrenderPhoneByWorkOrder(Long phoneId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("surrender")
                .fromValue(phone.getOrgId() != null ? phone.getOrgId().toString() : null)
                .toValue(null)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_SURRENDER")
                .title("号码回收工单 - " + phone.getPhoneNumber())
                .description("回收号码 " + phone.getPhoneNumber())
                .priority("MEDIUM")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone surrender work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO transferPhoneByWorkOrder(Long phoneId, Long fromOrgId, Long toOrgId, 
                                               Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("transfer")
                .fromValue(fromOrgId.toString())
                .toValue(toOrgId.toString())
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_TRANSFER")
                .title("号码过户工单 - " + phone.getPhoneNumber())
                .description("将号码 " + phone.getPhoneNumber() + " 从组织 " + fromOrgId + " 过户到组织 " + toOrgId)
                .priority("HIGH")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone transfer work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO changePhoneNumberByWorkOrder(Long phoneId, String newPhoneNumber,
                                                    Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("change_number")
                .fromValue(phone.getPhoneNumber())
                .toValue(newPhoneNumber)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_CHANGE_NUMBER")
                .title("号码变更工单 - " + phone.getPhoneNumber() + " -> " + newPhoneNumber)
                .description("将号码 " + phone.getPhoneNumber() + " 变更为 " + newPhoneNumber)
                .priority("MEDIUM")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone number change work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO changePhoneOrgByWorkOrder(Long phoneId, Long newOrgId,
                                                 Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("change_org")
                .fromValue(phone.getOrgId() != null ? phone.getOrgId().toString() : null)
                .toValue(newOrgId.toString())
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_CHANGE_ORG")
                .title("号码组织变更工单 - " + phone.getPhoneNumber())
                .description("将号码 " + phone.getPhoneNumber() + " 变更到组织 " + newOrgId)
                .priority("MEDIUM")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone org change work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO reclaimPhoneByWorkOrder(Long phoneId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("reclaim")
                .fromValue(phone.getOrgId() != null ? phone.getOrgId().toString() : null)
                .toValue(null)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_RECLAIM")
                .title("号码回收工单 - " + phone.getPhoneNumber())
                .description("强制回收号码 " + phone.getPhoneNumber())
                .priority("HIGH")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone reclaim work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO enablePhoneByWorkOrder(Long phoneId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("enable")
                .fromValue(phone.getStatus().name())
                .toValue("active")
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_ENABLE")
                .title("号码启用工单 - " + phone.getPhoneNumber())
                .description("启用号码 " + phone.getPhoneNumber())
                .priority("MEDIUM")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone enable work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO disablePhoneByWorkOrder(Long phoneId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneNumber phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("PHONE")
                .targetId(phoneId)
                .action("disable")
                .fromValue(phone.getStatus().name())
                .toValue("disabled")
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("PHONE_DISABLE")
                .title("号码停机工单 - " + phone.getPhoneNumber())
                .description("停机号码 " + phone.getPhoneNumber())
                .priority("HIGH")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Phone disable work order created: {} for phone {}", workOrder.getWorkOrderNo(), phoneId);
        
        return workOrder;
    }

    @Transactional
    public void executePhoneWorkOrderItem(WorkOrderItem item) {
        String action = item.getAction();
        Long phoneId = item.getTargetId();
        String operator = item.getOperator();
        String workOrderNo = null;

        switch (action) {
            case "allocate" -> {
                PhoneAllocationRequest request = new PhoneAllocationRequest();
                request.setPhoneId(phoneId);
                request.setOrgId(Long.parseLong(item.getToValue()));
                // Use the toValue as the target userId (item carries the target user/employee info)
            if (item.getToValue() != null && !item.getToValue().isEmpty()) {
                request.setUserId(item.getToValue());
            }
                phoneService.allocatePhone(request, operator);
            }
            case "surrender" -> {
                phoneService.surrenderPhone(phoneId, "WORK_ORDER", operator, workOrderNo, "工单回收");
            }
            case "reclaim" -> {
                PhoneReclaimRequest request = new PhoneReclaimRequest();
                request.setPhoneId(phoneId);
                request.setReason("WORK_ORDER");
                phoneService.reclaimPhone(request, operator);
            }
            case "transfer" -> {
                phoneService.changeUser(phoneId, item.getToValue(), operator, workOrderNo, "工单过户");
            }
            case "change_number" -> {
                phoneService.changeNumber(phoneId, item.getToValue(), operator, workOrderNo, "工单变更");
            }
            case "change_org" -> {
                phoneService.changeOrg(phoneId, Long.parseLong(item.getToValue()), operator, workOrderNo, "工单变更组织");
            }
            case "enable" -> {
                phoneService.changeStatus(phoneId, PhoneNumber.PhoneStatus.active, operator, workOrderNo, "工单启用");
            }
            case "disable" -> {
                phoneService.changeStatus(phoneId, PhoneNumber.PhoneStatus.disabled, operator, workOrderNo, "工单停机");
            }
            default -> throw new BusinessException(ErrorCode.SYS_001);
        }
    }
}

