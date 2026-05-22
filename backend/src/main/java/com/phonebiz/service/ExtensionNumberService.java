package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.entity.ExtensionNumber;
import com.phonebiz.entity.ExtensionNumber.ExtStatus;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.entity.WorkOrderItem;
import com.phonebiz.repository.ExtensionNumberRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.WorkOrderRepository;
import com.phonebiz.repository.WorkOrderItemRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtensionNumberService {

    private final ExtensionNumberRepository extRepo;
    private final OrgStructureRepository orgRepo;
    private final WorkOrderRepository woRepo;
    private final WorkOrderItemRepository woiRepo;

    public Page<ExtensionNumber> search(String keyword, ExtStatus status, Long deptOrgId, Pageable pageable) {
        return extRepo.searchOrdered(keyword, status, deptOrgId, pageable);
    }

    @Transactional
    public ExtensionNumber allocate(Long id, String userName, Long deptOrgId, String deptName, String phoneNumber, String operator) {
        ExtensionNumber ext = extRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("分机号不存在"));
        if (ext.getStatus() == ExtStatus.ALLOCATED) {
            throw new RuntimeException("该分机号已被占用");
        }

        ext.setUserName(userName);
        ext.setDeptOrgId(deptOrgId);
        ext.setDeptName(deptName);
        ext.setBranchName(resolveBranchName(deptOrgId));
        ext.setPhoneNumber(phoneNumber);
        ext.setStatus(ExtStatus.ALLOCATED);
        ext.setUpdatedBy(operator);

        extRepo.save(ext);

        WorkOrder wo = createWorkOrder("分机号分配 - " + ext.getExtensionNumber(),
                "将分机号 " + ext.getExtensionNumber() + " 分配给 " + userName + "（" + deptName + "）",
                WorkOrder.WorkOrderType.PHONE_ALLOCATE, operator, ext.getId(),
                null, ext.getExtensionNumber());
        ext.setWorkOrderId(wo.getId());
        extRepo.save(ext);

        return ext;
    }

    @Transactional
    public ExtensionNumber reclaim(Long id, String operator) {
        ExtensionNumber ext = extRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("分机号不存在"));
        if (ext.getStatus() != ExtStatus.ALLOCATED) {
            throw new RuntimeException("该分机号未被占用，无法回收");
        }

        String prevUser = ext.getUserName();
        String prevDept = ext.getDeptName();
        String prevExt = ext.getExtensionNumber();

        ext.setUserName(null);
        ext.setDeptOrgId(null);
        ext.setDeptName(null);
        ext.setBranchName(null);
        ext.setPhoneNumber(null);
        ext.setStatus(ExtStatus.IDLE);
        ext.setWorkOrderId(null);
        ext.setUpdatedBy(operator);

        extRepo.save(ext);

        createWorkOrder("分机号回收 - " + prevExt,
                "回收分机号 " + prevExt + "，原使用人: " + prevUser + "（" + prevDept + "）",
                WorkOrder.WorkOrderType.PHONE_RECLAIM, operator, ext.getId(),
                prevExt, null);

        return ext;
    }

    private WorkOrder createWorkOrder(String title, String desc,
                                       WorkOrder.WorkOrderType type, String operator,
                                       Long targetId, String fromValue, String toValue) {
        String woNo = "WO" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        WorkOrder wo = WorkOrder.builder()
                .workOrderNo(woNo)
                .type(type)
                .status(WorkOrder.WorkOrderStatus.COMPLETED)
                .priority(WorkOrder.WorkOrderPriority.NORMAL)
                .requesterName(operator)
                .handlerName(operator)
                .title(title)
                .description(desc)
                .completedAt(LocalDateTime.now())
                .build();
        wo.setCreatedBy(operator);
        wo.setUpdatedBy(operator);
        woRepo.save(wo);

        WorkOrderItem item = WorkOrderItem.builder()
                .workOrderId(wo.getId())
                .itemType(WorkOrderItem.ItemType.PHONE)
                .targetId(targetId)
                .action(type.name())
                .fromValue(fromValue)
                .toValue(toValue)
                .status(WorkOrderItem.ItemStatus.COMPLETED)
                .operator(operator)
                .executedAt(LocalDateTime.now())
                .build();
        item.setCreatedBy(operator);
        item.setUpdatedBy(operator);
        woiRepo.save(item);

        return wo;
    }

    private String resolveBranchName(Long deptOrgId) {
        if (deptOrgId == null) return null;
        var deptOpt = orgRepo.findById(deptOrgId);
        if (deptOpt.isEmpty()) return null;
        OrgStructure dept = deptOpt.get();
        if (dept.getPath() == null) return null;
        String[] segments = dept.getPath().split("/");
        if (segments.length < 3) return dept.getBranchName();
        try {
            Long lvl1Id = Long.parseLong(segments[2]);
            var branchOpt = orgRepo.findById(lvl1Id);
            return branchOpt.map(OrgStructure::getBranchName).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
