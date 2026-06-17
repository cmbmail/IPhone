package com.phonebiz.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.dto.PhoneOwnershipImportDTO;
import com.phonebiz.dto.PhoneOwnershipVO;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneOwnership;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneOwnershipRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneOwnershipService {

    private final PhoneOwnershipRepository phoneOwnershipRepository;
    private final OrgStructureRepository orgStructureRepository;

    // Cache org map - org structure rarely changes, refresh every 5 minutes
    private volatile Map<Long, OrgStructure> orgMapCache = null;
    private volatile long orgMapCacheTime = 0;
    private static final long ORG_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private Map<Long, OrgStructure> getOrgMap() {
        long now = System.currentTimeMillis();
        if (orgMapCache == null || (now - orgMapCacheTime) > ORG_CACHE_TTL_MS) {
            List<OrgStructure> allOrgs = orgStructureRepository.findAll();
            Map<Long, OrgStructure> map = new ConcurrentHashMap<>();
            for (OrgStructure o : allOrgs) { map.put(o.getId(), o); }
            orgMapCache = map;
            orgMapCacheTime = now;
        }
        return orgMapCache;
    }

    @Transactional(readOnly = true)
    public Page<PhoneOwnershipVO> search(String keyword, Long branchOrgId, Pageable pageable) {
        Page<PhoneOwnership> page = phoneOwnershipRepository.search(keyword, branchOrgId, pageable);
        // Use cached org map
        java.util.Map<Long, OrgStructure> orgMap = getOrgMap();

        return page.map(po -> {
            PhoneOwnershipVO vo = PhoneOwnershipVO.from(po);
            // Resolve allocation level columns from org tree
            Long deptId = po.getDeptOrgId() != null ? po.getDeptOrgId() : po.getBranchOrgId();
            if (deptId != null) {
                String[] levels = resolveLevels(deptId, orgMap);
                vo.setLevel1BranchName(levels[0]);
                vo.setLevel1BranchOrgId(levels[1] != null ? Long.parseLong(levels[1]) : null);
                vo.setLevel2OrgName(levels[2]);
                vo.setLevel2OrgId(levels[3] != null ? Long.parseLong(levels[3]) : null);
                vo.setLevel3OrgName(levels[4]);
                vo.setLevel3OrgId(levels[5] != null ? Long.parseLong(levels[5]) : null);
            }
            return vo;
        });
    }

    /**
     * Resolve 3-level allocation names from org tree path.
     * Returns [L1Name, L1Id, L2Name, L2Id, L3Name, L3Id]
     */
    private String[] resolveLevels(Long orgId, java.util.Map<Long, OrgStructure> orgMap) {
        // Build path from root to this org
        java.util.List<Long> path = new java.util.ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long cur = orgId;
        while (cur != null && !visited.contains(cur)) {
            path.add(0, cur);
            visited.add(cur);
            OrgStructure o = orgMap.get(cur);
            if (o == null) break;
            cur = o.getParentId();
        }
        // path[0]=root, path[1]=一级分行, path[2]=二级分行/部门/综合支行, path[3]=部门/支行/零专支行
        String l1Name = null, l1Id = null, l2Name = null, l2Id = null, l3Name = null, l3Id = null;
        if (path.size() > 1) {
            OrgStructure l1 = orgMap.get(path.get(1));
            if (l1 != null) { l1Name = l1.getName(); l1Id = String.valueOf(l1.getId()); }
        }
        if (path.size() > 2) {
            OrgStructure l2 = orgMap.get(path.get(2));
            if (l2 != null) { l2Name = l2.getName(); l2Id = String.valueOf(l2.getId()); }
        }
        if (path.size() > 3) {
            OrgStructure l3 = orgMap.get(path.get(3));
            if (l3 != null) { l3Name = l3.getName(); l3Id = String.valueOf(l3.getId()); }
        }
        return new String[]{l1Name, l1Id, l2Name, l2Id, l3Name, l3Id};
    }

    @Transactional(readOnly = true)
    public List<PhoneOwnership> listAll() {
        return phoneOwnershipRepository.findAllByOrderByPhoneNumberAsc();
    }

    @Transactional(readOnly = true)
    public List<PhoneOwnershipVO> listAllVO() {
        List<PhoneOwnership> all = phoneOwnershipRepository.findAllByOrderByPhoneNumberAsc();
        java.util.Map<Long, OrgStructure> orgMap = getOrgMap();

        return all.stream().map(po -> {
            PhoneOwnershipVO vo = PhoneOwnershipVO.from(po);
            Long deptId = po.getDeptOrgId() != null ? po.getDeptOrgId() : po.getBranchOrgId();
            if (deptId != null) {
                String[] levels = resolveLevels(deptId, orgMap);
                vo.setLevel1BranchName(levels[0]);
                vo.setLevel1BranchOrgId(levels[1] != null ? Long.parseLong(levels[1]) : null);
                vo.setLevel2OrgName(levels[2]);
                vo.setLevel2OrgId(levels[3] != null ? Long.parseLong(levels[3]) : null);
                vo.setLevel3OrgName(levels[4]);
                vo.setLevel3OrgId(levels[5] != null ? Long.parseLong(levels[5]) : null);
            }
            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public PhoneOwnership update(Long id, Long branchOrgId, Long deptOrgId, String remark) {
        PhoneOwnership po = phoneOwnershipRepository.findById(id)
                .orElseThrow(() -> new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.PHONE_001));

        if (branchOrgId != null) {
            OrgStructure branch = orgStructureRepository.findById(branchOrgId).orElse(null);
            if (branch != null) {
                po.setBranchOrgId(branch.getId());
                po.setBranchName(branch.getBranchName());
            }
        } else {
            po.setBranchOrgId(null);
            po.setBranchName(null);
        }

        if (deptOrgId != null) {
            OrgStructure dept = orgStructureRepository.findById(deptOrgId).orElse(null);
            if (dept != null) {
                po.setDeptOrgId(dept.getId());
                po.setDeptName(dept.getName());
            }
        } else {
            po.setDeptOrgId(null);
            po.setDeptName(null);
        }

        po.setRemark(remark);
        return phoneOwnershipRepository.save(po);
    }

    public List<PhoneOwnershipImportDTO> compareImport(MultipartFile file) throws Exception {
        List<PhoneOwnershipImportDTO> result = new ArrayList<>();
        Map<String, PhoneOwnership> existingMap = buildExistingMap();
        Map<String, OrgStructure> orgByName = buildOrgByNameMap();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] cols = line.split(",", -1);
                if (cols.length < 1 || cols[0].trim().isEmpty()) continue;

                String phone = cols[0].trim();
                String branchName = cols.length > 1 ? cols[1].trim() : "";
                String deptName = cols.length > 2 ? cols[2].trim() : "";
                String remark = cols.length > 3 ? cols[3].trim() : "";

                PhoneOwnership existing = existingMap.get(phone);

                PhoneOwnershipImportDTO dto = PhoneOwnershipImportDTO.builder()
                        .phoneNumber(phone)
                        .branchName(branchName)
                        .deptName(deptName)
                        .remark(remark)
                        .isNew(existing == null)
                        .hasDiff(false)
                        .build();

                if (existing != null) {
                    dto.setExistingBranchName(existing.getBranchName());
                    dto.setExistingDeptName(existing.getDeptName());
                    dto.setExistingRemark(existing.getRemark());

                    boolean diff = !Objects.equals(nullIfEmpty(branchName), nullIfEmpty(existing.getBranchName()))
                            || !Objects.equals(nullIfEmpty(deptName), nullIfEmpty(existing.getDeptName()))
                            || !Objects.equals(nullIfEmpty(remark), nullIfEmpty(existing.getRemark()));
                    dto.setHasDiff(diff);
                }
                result.add(dto);
            }
        }
        return result;
    }

    @Transactional
    public int confirmImport(List<PhoneOwnershipImportDTO> items) {
        Map<String, OrgStructure> orgByName = buildOrgByNameMap();
        int count = 0;
        for (PhoneOwnershipImportDTO dto : items) {
            Optional<PhoneOwnership> existingOpt = phoneOwnershipRepository.findByPhoneNumber(dto.getPhoneNumber());
            PhoneOwnership po;
            if (existingOpt.isPresent()) {
                po = existingOpt.get();
            } else {
                po = PhoneOwnership.builder().phoneNumber(dto.getPhoneNumber()).build();
            }

            // Resolve branch
            String branchName = dto.getBranchName();
            if (branchName != null && !branchName.isEmpty()) {
                OrgStructure branch = orgByName.get(branchName);
                if (branch != null) {
                    po.setBranchOrgId(branch.getId());
                    po.setBranchName(branch.getBranchName());
                } else {
                    po.setBranchOrgId(null);
                    po.setBranchName(branchName);
                }
            }

            // Resolve dept
            String deptName = dto.getDeptName();
            if (deptName != null && !deptName.isEmpty()) {
                OrgStructure dept = orgByName.get(deptName);
                if (dept != null) {
                    po.setDeptOrgId(dept.getId());
                    po.setDeptName(dept.getName());
                } else {
                    po.setDeptOrgId(null);
                    po.setDeptName(deptName);
                }
            }

            po.setRemark(dto.getRemark());
            phoneOwnershipRepository.save(po);
            count++;
        }
        return count;
    }

    private Map<String, PhoneOwnership> buildExistingMap() {
        Map<String, PhoneOwnership> map = new HashMap<>();
        phoneOwnershipRepository.findAll().forEach(p -> map.put(p.getPhoneNumber(), p));
        return map;
    }

    private Map<String, OrgStructure> buildOrgByNameMap() {
        Map<String, OrgStructure> map = new HashMap<>();
        orgStructureRepository.findAll().forEach(o -> {
            map.put(o.getName(), o);
            if (o.getBranchName() != null && !o.getBranchName().equals(o.getName())) {
                map.putIfAbsent(o.getBranchName(), o);
            }
        });
        return map;
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
