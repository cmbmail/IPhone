package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateCostCenterRequest;
import com.phonebiz.dto.UpdateCostCenterRequest;
import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.repository.OrgStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostCenterService {

    private final CostCenterMappingRepository costCenterRepository;
    private final OrgStructureRepository orgRepository;

    @Transactional(readOnly = true)
    public List<CostCenterMapping> getAllCostCenters() {
        return costCenterRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CostCenterMapping getCostCenterById(Long id) {
        return costCenterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED));
    }

    @Transactional(readOnly = true)
    public List<CostCenterMapping> getCostCentersByOrg(Long orgId) {
        return costCenterRepository.findByOrgId(orgId);
    }

    @Transactional
    public CostCenterMapping createCostCenter(CreateCostCenterRequest request, String operator) {
        OrgStructure org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        if (costCenterRepository.existsByOrgIdAndCostCenterCode(request.getOrgId(), request.getCostCenterCode())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                    "Cost center code already exists for this organization");
        }

        CostCenterMapping costCenter = new CostCenterMapping();
        costCenter.setOrgId(request.getOrgId());
        costCenter.setCostCenterName(request.getCostCenterName());
        costCenter.setCostCenterCode(request.getCostCenterCode());
        costCenter.setStatus(CostCenterMapping.CostCenterStatus.valueOf(request.getStatus()));
        costCenter.setCreatedBy(operator);
        costCenter.setUpdatedBy(operator);

        return costCenterRepository.save(costCenter);
    }

    @Transactional
    public CostCenterMapping updateCostCenter(Long id, UpdateCostCenterRequest request, String operator) {
        CostCenterMapping costCenter = getCostCenterById(id);

        if (request.getCostCenterName() != null) {
            costCenter.setCostCenterName(request.getCostCenterName());
        }

        if (request.getCostCenterCode() != null && !request.getCostCenterCode().equals(costCenter.getCostCenterCode())) {
            if (costCenterRepository.existsByOrgIdAndCostCenterCode(costCenter.getOrgId(), request.getCostCenterCode())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
            }
            costCenter.setCostCenterCode(request.getCostCenterCode());
        }

        if (request.getStatus() != null) {
            costCenter.setStatus(CostCenterMapping.CostCenterStatus.valueOf(request.getStatus()));
        }

        costCenter.setUpdatedBy(operator);
        return costCenterRepository.save(costCenter);
    }

    @Transactional
    public void deleteCostCenter(Long id) {
        costCenterRepository.deleteById(id);
    }
}
