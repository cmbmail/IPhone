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
import com.phonebiz.dto.WorkOrderDTO;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.repository.PhoneDeviceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderDrivenDeviceService {

    private final FeatureFlagService featureFlagService;
    private final ApplicationContext applicationContext;
    private final PhoneDeviceService phoneDeviceService;
    private final PhoneDeviceRepository phoneDeviceRepository;
    
    private WorkOrderService getWorkOrderService() {
        return applicationContext.getBean(WorkOrderService.class);
    }

    @Transactional
    public WorkOrderDTO assignDeviceByWorkOrder(Long deviceId, String employeeNo, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("assign")
                .fromValue(device.getAssignedTo())
                .toValue(employeeNo)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_ASSIGN")
                .title("话机分配工单 - MAC: " + device.getMacAddress())
                .description("将话机 " + device.getMacAddress() + " 分配给员工 " + employeeNo)
                .priority("NORMAL")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device assign work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO reclaimDeviceByWorkOrder(Long deviceId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("reclaim")
                .fromValue(device.getAssignedTo())
                .toValue(null)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_RECLAIM")
                .title("话机回收工单 - MAC: " + device.getMacAddress())
                .description("回收话机 " + device.getMacAddress())
                .priority("NORMAL")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device reclaim work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO repairDeviceByWorkOrder(Long deviceId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("repair")
                .fromValue(device.getStatus().name())
                .toValue(PhoneDevice.PhoneDeviceStatus.repairing.name())
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_REPAIR")
                .title("话机送修工单 - MAC: " + device.getMacAddress())
                .description("话机 " + device.getMacAddress() + " 送修")
                .priority("HIGH")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device repair work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO retireDeviceByWorkOrder(Long deviceId, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("retire")
                .fromValue(device.getStatus().name())
                .toValue(PhoneDevice.PhoneDeviceStatus.retired.name())
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_RETIRE")
                .title("话机报废工单 - MAC: " + device.getMacAddress())
                .description("话机 " + device.getMacAddress() + " 报废")
                .priority("HIGH")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device retire work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO bindPhoneToDeviceByWorkOrder(Long deviceId, String extensionNumber, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("bind_phone")
                .fromValue(null)
                .toValue(extensionNumber)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_BIND_PHONE")
                .title("话机绑定号码工单 - MAC: " + device.getMacAddress())
                .description("话机 " + device.getMacAddress() + " 绑定分机 " + extensionNumber)
                .priority("NORMAL")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device bind phone work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public WorkOrderDTO unbindPhoneFromDeviceByWorkOrder(Long deviceId, String extensionNumber, Long requesterId, String requesterName) {
        if (!featureFlagService.isFeatureEnabled(FeatureFlagService.FEATURE_WORK_ORDER_DRIVEN)) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        CreateWorkOrderRequest.WorkOrderItemRequest itemRequest = CreateWorkOrderRequest.WorkOrderItemRequest.builder()
                .itemType("DEVICE")
                .targetId(deviceId)
                .action("unbind_phone")
                .fromValue(extensionNumber)
                .toValue(null)
                .build();

        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .type("DEVICE_UNBIND_PHONE")
                .title("话机解绑号码工单 - MAC: " + device.getMacAddress())
                .description("话机 " + device.getMacAddress() + " 解绑分机 " + extensionNumber)
                .priority("NORMAL")
                .items(List.of(itemRequest))
                .build();

        WorkOrderDTO workOrder = getWorkOrderService().createWorkOrder(request, requesterId, requesterName);
        
        log.info("Device unbind phone work order created: {} for device {}", workOrder.getWorkOrderNo(), deviceId);
        
        return workOrder;
    }

    @Transactional
    public void executeDeviceWorkOrderItem(WorkOrderItem item) {
        String action = item.getAction();
        Long deviceId = item.getTargetId();
        String operator = item.getOperator();

        switch (action) {
            case "assign" -> {
                com.phonebiz.dto.AssignPhoneDeviceRequest assignRequest = new com.phonebiz.dto.AssignPhoneDeviceRequest();
                assignRequest.setEmployeeNo(item.getToValue());
                phoneDeviceService.assignDevice(deviceId, assignRequest);
            }
            case "reclaim" -> {
                phoneDeviceService.reclaimDevice(deviceId, "工单回收");
            }
            case "repair" -> {
                phoneDeviceService.repairDevice(deviceId, "工单送修");
            }
            case "retire" -> {
                phoneDeviceService.retireDevice(deviceId, "工单报废");
            }
            case "bind_phone" -> {
                // 需要从工单项获取分机号，这里假设 toValue 包含分机号
                com.phonebiz.dto.BindPhoneRequest bindRequest = new com.phonebiz.dto.BindPhoneRequest();
                bindRequest.setExtensionNumber(item.getToValue());
                phoneDeviceService.bindPhone(deviceId, bindRequest);
            }
            case "unbind_phone" -> {
                // P1-08: Unbind phone from device using extension number lookup
                String extensionNumber = item.getFromValue();
                if (extensionNumber != null) {
                    // Find mapping by extension and unbind
                    phoneDeviceService.unbindPhoneByExtension(deviceId, extensionNumber);
                } else {
                    // If no extension specified, unbind all
                    phoneDeviceService.unbindAllPhonesForDevice(deviceId);
                }
            }
            default -> throw new BusinessException(ErrorCode.SYS_001);
        }
    }
}

