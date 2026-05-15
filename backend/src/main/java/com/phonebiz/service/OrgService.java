package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateOrgRequest;
import com.phonebiz.dto.UpdateOrgRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.OrgStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgStructureRepository orgRepository;

    @Transactional(readOnly = true)
    public List<OrgStructure> getAllActiveOrgs() {
        return orgRepository.findAllActiveOrderByLevelAndName();
    }

    @Transactional(readOnly = true)
    public OrgStructure getOrgById(Long id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));
    }

    @Transactional(readOnly = true)
    public List<OrgStructure> getOrgTree() {
        List<OrgStructure> allOrgs = orgRepository.findAllActiveOrderByLevelAndName();
        return buildTree(allOrgs);
    }

    @Transactional
    public OrgStructure createOrg(CreateOrgRequest request, String operator) {
        validateDuplicateName(request.getParentId(), request.getName(), null);

        OrgStructure org = new OrgStructure();
        org.setParentId(request.getParentId());
        org.setName(request.getName());
        org.setType(OrgStructure.OrgType.valueOf(request.getType()));
        org.setStatus(OrgStructure.OrgStatus.valueOf(request.getStatus()));

        if (request.getParentId() != null) {
            OrgStructure parent = getOrgById(request.getParentId());
            org.setLevel(parent.getLevel() + 1);
        } else {
            org.setLevel(0);
        }

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
            org.setType(OrgStructure.OrgType.valueOf(request.getType()));
        }

        if (request.getStatus() != null) {
            org.setStatus(OrgStructure.OrgStatus.valueOf(request.getStatus()));
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

        orgRepository.delete(org);
    }

    @Transactional(readOnly = true)
    public List<OrgStructure> getChildren(Long parentId) {
        return orgRepository.findByParentIdAndStatus(parentId, OrgStructure.OrgStatus.active);
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

    private List<OrgStructure> buildTree(List<OrgStructure> allOrgs) {
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
}
