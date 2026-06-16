package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.entity.ExtensionNumber;
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
    private final EntityManager entityManager;

    /**
     * 状态动态计算规则:
     * 1. 电话号码为空 → 闲置(0)
     * 2. 电话号码不为空 + 有未完成工单引用该分机号 → 分配中(2)
     * 3. 电话号码不为空 + 无未完成工单 → 占用(1)
     */
    @Transactional(readOnly = true)
    public Page<ExtensionNumber> search(String keyword, Integer status, Long deptOrgId, Pageable pageable) {
        Page<ExtensionNumber> page = extRepo.searchOrdered(keyword, null, deptOrgId, pageable);

        // 批量查所有分机号ID，然后一次查出有关联未完成工单的集合
        List<ExtensionNumber> content = page.getContent();
        Set<Long> extIdsWithPendingWO = findExtIdsWithPendingWorkOrders(content);

        // 动态计算status (detach first to prevent Hibernate flush in readOnly TX)
        for (ExtensionNumber ext : content) {
            entityManager.detach(ext);
            ext.setStatus(computeStatus(ext, extIdsWithPendingWO));
        }

        // 如果有status筛选，在内存中过滤
        if (status != null) {
            List<ExtensionNumber> filtered = content.stream()
                    .filter(e -> e.getStatus().equals(status))
                    .toList();
            // 返回过滤后的子集 - 简单处理，分页可能不精确，但状态筛选场景够用
            return new org.springframework.data.domain.PageImpl<>(
                    filtered, pageable, filtered.size());
        }

        return page;
    }

    private int computeStatus(ExtensionNumber ext, Set<Long> extIdsWithPendingWO) {
        if (ext.getPhoneNumber() == null || ext.getPhoneNumber().isBlank()) {
            return ExtensionNumber.EXT_AVAILABLE; // 0=闲置
        }
        if (extIdsWithPendingWO.contains(ext.getId())) {
            return ExtensionNumber.EXT_ALLOCATING; // 2=分配中
        }
        return ExtensionNumber.EXT_OCCUPIED; // 1=占用
    }

    /**
     * 一次查询所有有未完成工单关联的分机号ID集合
     * 未完成 = work_order.status IN (0,1,2) AND work_order_item.item_type=1(号码) AND deleted_at IS NULL
     */
    @SuppressWarnings("unchecked")
    private Set<Long> findExtIdsWithPendingWorkOrders(List<ExtensionNumber> extensions) {
        if (extensions.isEmpty()) return Set.of();

        List<Long> extIds = extensions.stream().map(ExtensionNumber::getId).toList();

        String jpql = """
            SELECT DISTINCT i.targetRefId
            FROM WorkOrderItem i
            JOIN WorkOrder w ON i.workOrderId = w.id
            WHERE i.targetRefId IN :extIds
              AND i.itemType = 1
              AND w.status IN (0, 1, 2)
              AND i.deletedAt IS NULL
              AND w.deletedAt IS NULL
            """;

        Query query = entityManager.createQuery(jpql);
        query.setParameter("extIds", extIds);
        List<Long> results = query.getResultList();
        return new HashSet<>(results);
    }

    @Transactional
    public ExtensionNumber allocate(Long id, String userName, Long deptOrgId, String deptName, String phoneNumber, String operator) {
        ExtensionNumber ext = extRepo.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("分机号不存在"));

        ext.setEmployeeName(userName);
        ext.setDeptOrgId(deptOrgId);
        ext.setDeptName(deptName);
        ext.setBranchName(resolveBranchName(deptOrgId));
        ext.setPhoneNumber(phoneNumber);
        ext.setStatus(ExtensionNumber.EXT_AVAILABLE); // DB存0，展示时动态计算
        ext.setUpdatedBy(operator);
        extRepo.save(ext);

        WorkOrder wo = createWorkOrder("分机号分配 - " + ext.getExtensionNumber(),
                "将分机号 " + ext.getExtensionNumber() + " 分配给 " + userName + "（" + deptName + "）",
                WorkOrder.WO_TYPE_ADD, operator, ext.getId(),
                null, ext.getExtensionNumber());
        ext.setWorkOrderId(wo.getId());
        extRepo.save(ext);

        return ext;
    }

    @Transactional
    public ExtensionNumber reclaim(Long id, String operator) {
        ExtensionNumber ext = extRepo.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException("分机号不存在"));

        String prevEmployeeName = ext.getEmployeeName();
        String prevDept = ext.getDeptName();
        String prevExt = ext.getExtensionNumber();

        ext.setEmployeeName(null);
        ext.setDeptOrgId(null);
        ext.setDeptName(null);
        ext.setBranchName(null);
        ext.setPhoneNumber(null);
        ext.setStatus(ExtensionNumber.EXT_AVAILABLE);
        ext.setWorkOrderId(null);
        ext.setUpdatedBy(operator);
        extRepo.save(ext);

        createWorkOrder("分机号回收 - " + prevExt,
                "回收分机号 " + prevExt + "，原使用人: " + prevEmployeeName + "（" + prevDept + "）",
                WorkOrder.WO_TYPE_UNBIND, operator, ext.getId(),
                prevExt, null);

        return ext;
    }

    private WorkOrder createWorkOrder(String title, String desc,
                                       Integer type, String operator,
                                       Long targetId, String fromValue, String toValue) {
        String woNo = "WO" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        WorkOrder wo = WorkOrder.builder()
                .workOrderNo(woNo)
                .type(type)
                .status(WorkOrder.WO_PENDING)
                .priority(WorkOrder.WO_NORMAL)
                .requesterName(operator)
                .handlerName(operator)
                .title(title)
                .description(desc)
                .build();
        wo.setCreatedBy(operator);
        wo.setUpdatedBy(operator);
        woRepo.save(wo);

        WorkOrderItem item = WorkOrderItem.builder()
                .workOrderId(wo.getId())
                .itemType(WorkOrderItem.ITEM_PHONE)
                .targetRefId(targetId)
                .action(String.valueOf(type))
                .fromValue(fromValue)
                .toValue(toValue)
                .status(WorkOrderItem.ITEM_PENDING)
                .operator(operator)
                .build();
        item.setCreatedBy(operator);
        item.setUpdatedBy(operator);
        woiRepo.save(item);

        return wo;
    }

    private String resolveBranchName(Long deptOrgId) {
        if (deptOrgId == null) return null;
        OrgStructure dept = orgRepo.findById(deptOrgId).orElse(null);
        if (dept == null) return null;
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
