package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateOrgRequest;
import com.phonebiz.dto.ImportOrgItem;
import com.phonebiz.dto.UpdateOrgRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.OrgStructureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final EntityManager entityManager;

    private final OrgStructureRepository orgRepository;

    @Transactional(readOnly = true)
    public List<OrgStructure> getAllActiveOrgs() {
        return orgRepository.findAllActiveOrdered();
    }

    @Transactional(readOnly = true)
    public OrgStructure getOrgById(Long id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));
    }

    @Transactional(readOnly = true)
    public List<OrgStructure> getOrgTree() {
        List<OrgStructure> allOrgs = orgRepository.findAllActiveOrdered();
        return buildTree(allOrgs);
    }

    @Transactional
    public OrgStructure createOrg(CreateOrgRequest request, String operator) {
        validateDuplicateName(request.getParentId(), request.getName(), null);

        OrgStructure org = new OrgStructure();
        org.setParentId(request.getParentId());
        org.setName(request.getName());
        org.setRemark(request.getRemark());
        org.setBranchName(request.getBranchName());
        org.setOrgCode(request.getOrgCode());
        org.setCostCenterCode(request.getCostCenterCode());
        
        if (request.getType() != null) {
            int newType = Integer.valueOf(request.getType());
            // Validate type compatibility with parent if type is changing
            if (org.getParentId() != null && newType != org.getType()) {
                OrgStructure parent = getOrgById(org.getParentId());
                parent.validateChildType(newType);
            }
            org.setType(newType);
        }

        if (request.getParentId() != null) {
            checkCycle(null, request.getParentId());
            OrgStructure parent = getOrgById(request.getParentId());
            org.setLevel(parent.getLevel() + 1);
            // Validate parent-child type compatibility
            parent.validateChildType(org.getType());
        } else {
            org.setLevel(0);
        }

        // Set sort_order: max sibling order + 10
        List<OrgStructure> siblings = org.getParentId() == null
                ? orgRepository.findByParentIdIsNull()
                : orgRepository.findByParentId(org.getParentId());
        int maxSort = siblings.stream().mapToInt(OrgStructure::getSortOrder).max().orElse(0);
        org.setSortOrder(maxSort + 10);

        // Set temporary path first (NOT NULL constraint), then update after save
        org.setPath(org.getParentId() != null ? "/" + org.getParentId() + "/0" : "/0");
        OrgStructure saved = orgRepository.save(org);
        saved.setPath(calculatePath(saved));
        return orgRepository.save(saved);
    }

    @Transactional
    public OrgStructure updateOrg(Long id, UpdateOrgRequest request, String operator) {
        OrgStructure org = getOrgById(id);

        if (request.getName() != null && !request.getName().equals(org.getName())) {
            validateDuplicateName(org.getParentId(), request.getName(), id);
            org.setName(request.getName());
        }

        if (request.getType() != null) {
            org.setType(Integer.valueOf(request.getType()));
        }

        if (request.getStatus() != null) {
            org.setStatus(Integer.valueOf(request.getStatus()));
        }

        if (request.getSortOrder() != null) {
            int position = request.getSortOrder(); // 1-based position
            // Get all siblings (same parent), exclude current org
            List<OrgStructure> siblings = org.getParentId() == null
                    ? orgRepository.findByParentIdIsNull()
                    : orgRepository.findByParentId(org.getParentId());
            siblings.removeIf(s -> s.getId().equals(org.getId()));
            siblings.sort((a, b) -> a.getSortOrder() - b.getSortOrder());

            // Clamp position to valid range [1, siblings.size()+1]
            position = Math.max(1, Math.min(position, siblings.size() + 1));

            // Insert current org into the siblings list at the desired position
            siblings.add(position - 1, org);

            // Reassign sort_order for ALL nodes (siblings + current): position × 10
            for (int i = 0; i < siblings.size(); i++) {
                int newOrder = (i + 1) * 10;
                siblings.get(i).setSortOrder(newOrder);
                orgRepository.updateSortOrder(siblings.get(i).getId(), newOrder);
            }
        }

        if (request.getBranchName() != null) {
            org.setBranchName(request.getBranchName());
        }
        if (request.getOrgCode() != null) {
            org.setOrgCode(request.getOrgCode());
        }
        if (request.getCostCenterCode() != null) {
            org.setCostCenterCode(request.getCostCenterCode());
        }

        org.setUpdatedBy(operator);
        return orgRepository.save(org);
    }

    @Transactional
    public void deleteOrg(Long id) {
        OrgStructure org = getOrgById(id);

        long childCount = orgRepository.countByParentId(id);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.ORG_004, "Cannot delete organization with children");
        }

        org.setDeletedAt(LocalDateTime.now()); orgRepository.save(org);
    }

    @Transactional(readOnly = true)
    public List<OrgStructure> getChildren(Long parentId) {
        return orgRepository.findByParentIdAndStatus(parentId, OrgStructure.ORG_ACTIVE);
    }

    @Transactional
    public void updateSortOrders(Map<Long, Integer> sortOrderMap) {
        for (Map.Entry<Long, Integer> entry : sortOrderMap.entrySet()) {
            orgRepository.updateSortOrder(entry.getKey(), entry.getValue());
        }
    }

    private void validateDuplicateName(Long parentId, String name, Long excludeId) {
        boolean exists;
        if (parentId == null) {
            exists = orgRepository.existsByParentIdIsNullAndName(name);
        } else {
            exists = orgRepository.existsByParentIdAndName(parentId, name);
        }

        if (exists) {
            throw new BusinessException(ErrorCode.ORG_002);
        }
    }

    private String calculatePath(OrgStructure org) {
        if (org.getParentId() == null) {
            return "/" + org.getId();
        }
        OrgStructure parent = getOrgById(org.getParentId());
        return parent.getPath() + "/" + org.getId();
    }


    @Transactional
    public int importOrgs(List<ImportOrgItem> items, String operator) {
        int count = 0;
        Map<String, Long> nameToId = new HashMap<>();
        for (OrgStructure o : orgRepository.findAll()) {
            nameToId.put(o.getName(), o.getId());
        }
        for (ImportOrgItem item : items) {
            if (item.getName() == null || item.getName().isBlank()) continue;
            String name = item.getName().trim();
            String parentName = item.getParentId() != null ? String.valueOf(item.getParentId()) : null;
            Long pid = null;
            if (parentName != null && !parentName.isEmpty()) {
                try { pid = Long.parseLong(parentName); } catch (NumberFormatException e) { pid = null; }
                if (pid == null) pid = nameToId.get(parentName);
                if (pid == null) { log.warn("Parent not found for '{}', skipping", name); continue; }
            }
            boolean exists = (pid == null) ? orgRepository.existsByParentIdIsNullAndName(name) : orgRepository.existsByParentIdAndName(pid, name);
            if (exists) { log.info("Skip duplicate org: {} under parent {}", name, pid); continue; }
            OrgStructure org = new OrgStructure();
            org.setParentId(pid);
            org.setName(name);
            if (item.getType() != null && !item.getType().isBlank()) {
                org.setType(Integer.valueOf(item.getType()));
            } else if (pid == null) {
                org.setType(OrgStructure.ORG_GROUP);
            } else {
                OrgStructure parent = orgRepository.findById(pid).orElse(null);
                if (parent != null) {
                    org.setType(deriveChildType(parent.getType()));
                } else {
                    org.setType(OrgStructure.ORG_DEPT);
                }
            }
            org.setLevel(pid != null ? (orgRepository.findById(pid).map(o -> o.getLevel() + 1).orElse(0)) : 0);
            // Set sort_order
            List<OrgStructure> siblings = org.getParentId() == null
                    ? orgRepository.findByParentIdIsNull()
                    : orgRepository.findByParentId(org.getParentId());
            int maxSort = siblings.stream().mapToInt(OrgStructure::getSortOrder).max().orElse(0);
            org.setSortOrder(maxSort + 10);
            OrgStructure saved = orgRepository.save(org);
            saved.setPath(calculatePath(saved));
            orgRepository.save(saved);
            nameToId.put(name, saved.getId());
            count++;
        }
        return count;
    }


    @Transactional
    public Map<String, Object> importCostCenter(MultipartFile file, String operator) {
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        // M-13: Validate Excel file magic bytes (PK header for xlsx)
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4 || fileBytes[0] != 0x50 || fileBytes[1] != 0x4B) {
                throw new BusinessException(ErrorCode.SYS_001, "Invalid Excel file format");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYS_001, "Failed to read file");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String deptName = getCellStringValue(row, 0);
                if (deptName == null || deptName.isBlank()) continue;
                deptName = deptName.trim();

                String branchName = getCellStringValue(row, 1);
                String orgCode = getCellStringValue(row, 2);
                String costCenter = getCellStringValue(row, 3);

                Optional<OrgStructure> opt = orgRepository.findByName(deptName);
                if (opt.isEmpty()) {
                    skipped++;
                    errors.add("Row " + (i+1) + ": Department '" + deptName + "' not found");
                    continue;
                }

                OrgStructure org = opt.get();
                if (branchName != null && !branchName.isBlank()) org.setBranchName(branchName.trim());
                if (orgCode != null && !orgCode.isBlank()) org.setOrgCode(orgCode.trim());
                if (costCenter != null && !costCenter.isBlank()) org.setCostCenterCode(costCenter.trim());
                org.setUpdatedBy(operator);
                orgRepository.save(org);
                updated++;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMPORT_002, "Failed to parse Excel: " + e.getMessage());
        }

        return Map.of("updated", updated, "skipped", skipped, "errors", errors);
    }

    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default: return null;
        }
    }

    @Transactional(readOnly = true)
    public List<OrgStructure> getOrgTreeByScope(Long scopeOrgId) {
        List<OrgStructure> allOrgs;
        if (scopeOrgId == null) {
            allOrgs = orgRepository.findAllActiveOrdered();
        } else {
            OrgStructure scopeOrg = getOrgById(scopeOrgId);
            allOrgs = orgRepository.findByPathStartingWith(scopeOrg.getPath());
            allOrgs.add(scopeOrg);
            allOrgs.sort(Comparator.comparingInt(OrgStructure::getSortOrder)
                    .thenComparing(OrgStructure::getName));
        }
        return buildTree(allOrgs);
    }

    private List<OrgStructure> buildTree(List<OrgStructure> allOrgs) {
        // Detach all entities first to prevent Hibernate flush on collection mutations
        for (OrgStructure org : allOrgs) {
            entityManager.detach(org);
        }
        // Clear JPA EAGER-loaded children first to avoid duplication
        for (OrgStructure org : allOrgs) {
            org.getChildren().clear();
        }

        Map<Long, OrgStructure> map = allOrgs.stream()
                .collect(Collectors.toMap(OrgStructure::getId, o -> o));

        List<OrgStructure> roots = new ArrayList<>();

        for (OrgStructure org : allOrgs) {
            if (org.getParentId() == null) {
                roots.add(org);
            } else {
                OrgStructure parent = map.get(org.getParentId());
                if (parent != null) {
                    parent.getChildren().add(org);
                }
            }
        }

        return roots;
    }

    /** S22: Check if making newParentId the parent of orgId would create a cycle */
    private void checkCycle(Long orgId, Long newParentId) {
        if (newParentId == null) return;
        if (newParentId.equals(orgId)) {
            throw new BusinessException(ErrorCode.ORG_003, "Cannot set self as parent");
        }
        Set<Long> visited = new HashSet<>();
        Long current = newParentId;
        while (current != null) {
            if (current.equals(orgId)) {
                throw new BusinessException(ErrorCode.ORG_003, "Cycle detected in organization structure");
            }
            if (visited.contains(current)) {
                throw new BusinessException(ErrorCode.ORG_003, "Cycle detected in organization structure");
            }
            visited.add(current);
            OrgStructure parent = orgRepository.findById(current).orElse(null);
            current = parent != null ? parent.getParentId() : null;
        }
    }

    /**
     * Derive the default child type based on parent type.
     *   集团(1) → 分行(2)
     *   分行(2) → 部门(3) (default, can also be 分行/综合支行)
     *   部门(3) → 部门(3)
     *   综合支行(4) → 零专支行(5)
     *   零专支行(5) → no children (returns dept as fallback)
     */
    private int deriveChildType(int parentType) {
        return switch (parentType) {
            case OrgStructure.ORG_GROUP -> OrgStructure.ORG_BRANCH;
            case OrgStructure.ORG_BRANCH -> OrgStructure.ORG_DEPT;
            case OrgStructure.ORG_DEPT -> OrgStructure.ORG_DEPT;
            case OrgStructure.ORG_COMP_SUB -> OrgStructure.ORG_RETL_SUB;
            default -> OrgStructure.ORG_DEPT;
        };
    }
}