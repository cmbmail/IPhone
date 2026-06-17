package com.phonebiz.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.dto.OwnershipLevelDTO;
import com.phonebiz.dto.PhoneOwnershipImportDTO;
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

    @Transactional(readOnly = true)
    public Page<PhoneOwnership> search(String keyword, Long branchOrgId, Pageable pageable) {
        return phoneOwnershipRepository.search(keyword, branchOrgId, pageable);
    }

    @Transactional(readOnly = true)
    public List<PhoneOwnership> listAll() {
        return phoneOwnershipRepository.findAllByOrderByPhoneNumberAsc();
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

    // ==================== Allocation Level Queries ====================

    /**
     * Get phone ownership grouped by allocation level.
     * Level 1: 一级分行 (direct children of root, type=BRANCH)
     * Level 2: 二级分行/一级分行部门/综合支行 (direct children of Level 1)
     * Level 3: 二级分行部门/支行/零专支行/子部门 (direct children of Level 2)
     */
    @Transactional(readOnly = true)
    public com.phonebiz.dto.OwnershipLevelDTO.LevelSummaryResponse getOwnershipByLevel(int level, Long parentOrgId) {
        List<OrgStructure> allOrgs = orgStructureRepository.findAll();
        java.util.Map<Long, OrgStructure> orgMap = new java.util.HashMap<>();
        for (OrgStructure o : allOrgs) {
            orgMap.put(o.getId(), o);
        }

        // Build phoneOwnership list mapped by deptOrgId
        List<PhoneOwnership> allPhones = phoneOwnershipRepository.findAllByOrderByPhoneNumberAsc();
        java.util.Map<Long, java.util.List<PhoneOwnership>> phonesByDept = new java.util.HashMap<>();
        for (PhoneOwnership po : allPhones) {
            Long deptId = po.getDeptOrgId() != null ? po.getDeptOrgId() : po.getBranchOrgId();
            if (deptId != null) {
                phonesByDept.computeIfAbsent(deptId, k -> new java.util.ArrayList<>()).add(po);
            }
        }

        // Group orgs by allocation level
        java.util.Map<Long, java.util.List<Long>> levelGroups = new java.util.LinkedHashMap<>();
        // For each org, determine which Level1/Level2 org it belongs to
        for (OrgStructure org : allOrgs) {
            Long targetId = resolveAllocationTarget(org, level, orgMap);
            if (targetId != null) {
                levelGroups.computeIfAbsent(targetId, k -> new java.util.ArrayList<>()).add(org.getId());
            }
        }

        // If parentOrgId specified, filter to only that parent's children
        java.util.List<OwnershipLevelDTO> items = new java.util.ArrayList<>();
        int totalPhones = 0;
        int totalAllocated = 0;

        java.util.Set<Long> targetIds = new java.util.LinkedHashSet<>();
        if (parentOrgId != null) {
            // Filter: only include targets whose parent matches the given parentOrgId
            for (java.util.Map.Entry<Long, java.util.List<Long>> entry : levelGroups.entrySet()) {
                OrgStructure targetOrg = orgMap.get(entry.getKey());
                if (targetOrg != null && parentOrgId.equals(targetOrg.getParentId())) {
                    targetIds.add(entry.getKey());
                }
            }
        } else {
            targetIds = levelGroups.keySet();
        }

        for (Long targetId : targetIds) {
            java.util.List<Long> deptIds = levelGroups.get(targetId);
            if (deptIds == null || deptIds.isEmpty()) continue;

            OrgStructure targetOrg = orgMap.get(targetId);
            String targetName = targetOrg != null ? targetOrg.getName() : "未知";
            Integer targetType = targetOrg != null ? targetOrg.getType() : 0;
            Long parentOrgIdVal = targetOrg != null ? targetOrg.getParentId() : null;
            String parentName = null;
            if (parentOrgIdVal != null) {
                OrgStructure parentOrg = orgMap.get(parentOrgIdVal);
                parentName = parentOrg != null ? parentOrg.getName() : "招商银行";
            } else {
                parentName = "招商银行";
            }

            int phoneCount = 0;
            int allocatedCount = 0;
            for (Long deptId : deptIds) {
                java.util.List<PhoneOwnership> phones = phonesByDept.get(deptId);
                if (phones != null) {
                    phoneCount += phones.size();
                    allocatedCount += (int) phones.stream().filter(p -> p.getDeptOrgId() != null).count();
                }
            }

            totalPhones += phoneCount;
            totalAllocated += allocatedCount;

            items.add(OwnershipLevelDTO.builder()
                    .orgId(targetId)
                    .orgName(targetName)
                    .orgType(targetType)
                    .orgTypeName(getOrgTypeName(targetType))
                    .parentOrgId(parentOrgIdVal)
                    .parentOrgName(parentName)
                    .phoneCount(phoneCount)
                    .allocatedCount(allocatedCount)
                    .build());
        }

        // Sort by phoneCount desc
        items.sort((a, b) -> b.getPhoneCount() - a.getPhoneCount());

        String levelName = switch (level) {
            case 1 -> "一级分行";
            case 2 -> "二级分行/一级部门/综合支行";
            case 3 -> "二级分行部门/支行/零专支行";
            default -> "未知";
        };
        String levelDesc = switch (level) {
            case 1 -> "总行 → 一级分行";
            case 2 -> "一级分行 → 二级分行/部门/综合支行";
            case 3 -> "二级分行/部门/综合支行 → 子部门/支行/零专支行";
            default -> "";
        };

        return com.phonebiz.dto.OwnershipLevelDTO.LevelSummaryResponse.builder()
                .level(level)
                .levelName(levelName)
                .levelDescription(levelDesc)
                .totalOrgs(items.size())
                .totalPhones(totalPhones)
                .totalAllocated(totalAllocated)
                .items(items)
                .build();
    }

    private Long resolveAllocationTarget(OrgStructure org, int targetLevel, java.util.Map<Long, OrgStructure> orgMap) {
        // Build path from root to this org
        java.util.List<Long> path = new java.util.ArrayList<>();
        Long currentId = org.getId();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        while (currentId != null && !visited.contains(currentId)) {
            path.add(0, currentId);
            visited.add(currentId);
            OrgStructure current = orgMap.get(currentId);
            if (current == null) break;
            currentId = current.getParentId();
        }

        // path[0] = root (集团), path[1] = 一级分行, path[2] = Level2 target, path[3] = Level3 target
        if (path.size() > targetLevel) {
            return path.get(targetLevel);
        }
        return null;
    }

    private String getOrgTypeName(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case 1 -> "集团";
            case 2 -> "分行";
            case 3 -> "部门";
            case 4 -> "综合支行";
            case 5 -> "零专支行";
            default -> "未知";
        };
    }
}
