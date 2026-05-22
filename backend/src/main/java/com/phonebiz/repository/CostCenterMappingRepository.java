package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.CostCenterMapping;

@Repository
public interface CostCenterMappingRepository extends JpaRepository<CostCenterMapping, Long> {

    List<CostCenterMapping> findByOrgId(Long orgId);

    List<CostCenterMapping> findByStatus(Integer status);

    Optional<CostCenterMapping> findByOrgIdAndCostCenterCode(Long orgId, String costCenterCode);

    boolean existsByOrgIdAndCostCenterCode(Long orgId, String costCenterCode);
}

