package com.phonebiz.repository;

import com.phonebiz.entity.CostCenterMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterMappingRepository extends JpaRepository<CostCenterMapping, Long> {

    List<CostCenterMapping> findByOrgId(Long orgId);

    List<CostCenterMapping> findByStatus(CostCenterMapping.CostCenterStatus status);

    Optional<CostCenterMapping> findByOrgIdAndCostCenterCode(Long orgId, String costCenterCode);

    boolean existsByOrgIdAndCostCenterCode(Long orgId, String costCenterCode);
}
