package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateAreaCodeMappingRequest;
import com.phonebiz.entity.AreaCodeOrgMapping;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.AreaCodeOrgMappingRepository;
import com.phonebiz.repository.OrgStructureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaCodeService {

    private final AreaCodeOrgMappingRepository mappingRepository;
    private final OrgStructureRepository orgRepository;

    @Transactional(readOnly = true)
    public List<AreaCodeOrgMapping> getAllMappings() {
        return mappingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AreaCodeOrgMapping getMappingById(Long id) {
        return mappingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED));
    }

    @Transactional(readOnly = true)
    public List<AreaCodeOrgMapping> getMappingsByAreaCode(String areaCode) {
        return mappingRepository.findByAreaCodeOrderByPriority(areaCode);
    }

    @Transactional(readOnly = true)
    public List<AreaCodeOrgMapping> getMappingsByOrg(Long orgId) {
        return mappingRepository.findByOrgId(orgId);
    }

    @Transactional(readOnly = true)
    public Long matchOrgByAreaCode(String areaCode) {
        return mappingRepository.findFirstByAreaCodeOrderByPriorityAsc(areaCode)
                .map(AreaCodeOrgMapping::getOrgId)
                .orElse(null);
    }

    @Transactional
    public AreaCodeOrgMapping createMapping(CreateAreaCodeMappingRequest request, String operator) {
        OrgStructure org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        if (mappingRepository.existsByAreaCodeAndOrgId(request.getAreaCode(), request.getOrgId())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                    "Area code already mapped to this organization");
        }

        AreaCodeOrgMapping mapping = new AreaCodeOrgMapping();
        mapping.setAreaCode(request.getAreaCode());
        mapping.setOrgId(request.getOrgId());
        mapping.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        mapping.setCreatedBy(operator);
        mapping.setUpdatedBy(operator);

        return mappingRepository.save(mapping);
    }

    @Transactional
    public void deleteMapping(Long id) {
        mappingRepository.findById(id).ifPresent(e -> { e.setDeletedAt(LocalDateTime.now()); mappingRepository.save(e); });
    }
}

