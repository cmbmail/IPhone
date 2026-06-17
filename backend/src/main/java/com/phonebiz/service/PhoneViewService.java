package com.phonebiz.service;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.phonebiz.dto.PhoneViewDTO;
import com.phonebiz.entity.ExtensionNumber;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.ExtensionNumberRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneDeviceRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneViewService {

    private final PhoneNumberRepository phoneRepo;
    private final ExtensionNumberRepository extRepo;
    private final PhoneDeviceRepository deviceRepo;
    private final OrgStructureRepository orgRepo;
    private final EntityManager entityManager;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_OCCUPIED = 1;
    public static final int STATUS_ALLOCATING = 2;

    @Transactional(readOnly = true)
    public Page<PhoneViewDTO> listPhones(String keyword, Integer status, Long orgId, Pageable pageable) {
        // 1. 查分页电话号码
        String countJpql = "SELECT COUNT(p) FROM PhoneNumber p WHERE p.deletedAt IS NULL";
        String dataJpql = "SELECT p FROM PhoneNumber p WHERE p.deletedAt IS NULL";

        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.phoneNumber LIKE :kw OR p.employeeNo LIKE :kw)");
            params.put("kw", "%" + keyword + "%");
        }
        if (orgId != null) {
            where.append(" AND p.orgId = :orgId");
            params.put("orgId", orgId);
        }

        jakarta.persistence.Query countQuery = entityManager.createQuery(countJpql + where);
        params.forEach(countQuery::setParameter);
        long total = (Long) countQuery.getSingleResult();

        jakarta.persistence.Query dataQuery = entityManager.createQuery(dataJpql + where + " ORDER BY p.phoneNumber ASC");
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<PhoneNumber> phones = dataQuery.getResultList();

        if (phones.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 2. 批量查关联数据
        List<Long> phoneIds = phones.stream().map(PhoneNumber::getId).toList();

        // extension_number by phone_id
        Map<Long, List<ExtensionNumber>> extMap = extRepo.findAllByPhoneIdIn(phoneIds)
                .stream().collect(Collectors.groupingBy(ExtensionNumber::getPhoneId));

        // 收集所有分机号 → 查 phone_device 的 MAC
        List<String> allExtNumbers = extMap.values().stream()
                .flatMap(Collection::stream)
                .map(ExtensionNumber::getExtensionNumber)
                .toList();

        Map<String, List<String>> extToMacs = new HashMap<>();
        if (!allExtNumbers.isEmpty()) {
            List<PhoneDevice> devices = deviceRepo.findByExtensionNumberIn(allExtNumbers);
            for (PhoneDevice d : devices) {
                extToMacs.computeIfAbsent(d.getExtensionNumber(), k -> new ArrayList<>())
                        .add(d.getMacAddress());
            }
        }

        // 有未完成工单的电话号码ID集合
        Set<Long> phoneIdsWithPendingWO = findPhoneIdsWithPendingWorkOrders(phoneIds, extMap);

        // 3. Preload org map for batch lookup
        java.util.Map<Long, OrgStructure> orgMap = new java.util.HashMap<>();
        orgRepo.findAll().forEach(o -> orgMap.put(o.getId(), o));

        // 4. 组装 DTO
        List<PhoneViewDTO> dtos = new ArrayList<>();
        for (PhoneNumber p : phones) {
            List<ExtensionNumber> exts = extMap.getOrDefault(p.getId(), List.of());
            List<String> extNumbers = exts.stream().map(ExtensionNumber::getExtensionNumber).toList();

            // 收集该电话号码关联的所有MAC
            List<String> allMacs = extNumbers.stream()
                    .flatMap(ext -> extToMacs.getOrDefault(ext, List.of()).stream())
                    .toList();

            String branchName = exts.stream().map(ExtensionNumber::getBranchName).filter(Objects::nonNull).findFirst().orElse(null);
            String deptName = exts.stream().map(ExtensionNumber::getDeptName).filter(Objects::nonNull).findFirst().orElse(null);
            String employeeName = exts.stream().map(ExtensionNumber::getEmployeeName).filter(Objects::nonNull).findFirst().orElse(null);

            if (branchName == null && p.getOrgId() != null) {
                OrgStructure org = orgMap.get(p.getOrgId());
                if (org != null) branchName = org.getBranchName();
            }
            if (deptName == null && p.getOrgId() != null) {
                OrgStructure org = orgMap.get(p.getOrgId());
                if (org != null) deptName = org.getName();
            }

            int computedStatus = computeStatus(extNumbers, p.getId(), phoneIdsWithPendingWO);

            dtos.add(PhoneViewDTO.builder()
                    .id(p.getId())
                    .phoneNumber(p.getPhoneNumber())
                    .employeeNo(p.getEmployeeNo())
                    .employeeName(employeeName)
                    .extensions(extNumbers)
                    .macAddresses(allMacs)
                    .branchName(branchName)
                    .deptName(deptName)
                    .status(computedStatus)
                    .orgId(p.getOrgId())
                    .build());
        }

        // status 筛选
        if (status != null) {
            dtos = dtos.stream().filter(d -> d.getStatus().equals(status)).toList();
            total = dtos.size();
        }

        return new PageImpl<>(dtos, pageable, total);
    }

    private int computeStatus(List<String> extensions, Long phoneId, Set<Long> phoneIdsWithPendingWO) {
        if (extensions.isEmpty()) {
            return STATUS_IDLE;
        }
        if (phoneIdsWithPendingWO.contains(phoneId)) {
            return STATUS_ALLOCATING;
        }
        return STATUS_OCCUPIED;
    }

    @SuppressWarnings("unchecked")
    private Set<Long> findPhoneIdsWithPendingWorkOrders(List<Long> phoneIds, Map<Long, List<ExtensionNumber>> extMap) {
        if (phoneIds.isEmpty()) return Set.of();

        Set<Long> result = new HashSet<>();

        // 直接关联: work_order_item.target_ref_id = phone_number.id
        String jpql1 = """
            SELECT DISTINCT i.targetRefId FROM WorkOrderItem i
            JOIN WorkOrder w ON i.workOrderId = w.id
            WHERE i.targetRefId IN :phoneIds AND i.itemType = 1
              AND w.status IN (0,1,2) AND i.deletedAt IS NULL AND w.deletedAt IS NULL
            """;
        jakarta.persistence.Query q1 = entityManager.createQuery(jpql1);
        q1.setParameter("phoneIds", phoneIds);
        result.addAll(q1.getResultList());

        // 通过分机号反查
        List<Long> allExtIds = extMap.values().stream()
                .flatMap(Collection::stream)
                .map(ExtensionNumber::getId)
                .toList();
        if (!allExtIds.isEmpty()) {
            String jpql2 = """
                SELECT DISTINCT i.targetRefId FROM WorkOrderItem i
                JOIN WorkOrder w ON i.workOrderId = w.id
                WHERE i.targetRefId IN :extIds AND i.itemType = 1
                  AND w.status IN (0,1,2) AND i.deletedAt IS NULL AND w.deletedAt IS NULL
                """;
            jakarta.persistence.Query q2 = entityManager.createQuery(jpql2);
            q2.setParameter("extIds", allExtIds);
            Set<Long> extIdsWithWO = new HashSet<>(q2.getResultList());

            Map<Long, Long> extToPhone = extMap.values().stream()
                    .flatMap(Collection::stream)
                    .filter(e -> e.getPhoneId() != null)
                    .collect(Collectors.toMap(ExtensionNumber::getId, ExtensionNumber::getPhoneId));
            for (Long extId : extIdsWithWO) {
                Long phoneId = extToPhone.get(extId);
                if (phoneId != null) result.add(phoneId);
            }
        }

        return result;
    }

    private String resolveBranchName(Long orgId) {
        return orgRepo.findById(orgId).map(OrgStructure::getBranchName).orElse(null);
    }

    private String resolveDeptName(Long orgId) {
        return orgRepo.findById(orgId).map(OrgStructure::getName).orElse(null);
    }
}
