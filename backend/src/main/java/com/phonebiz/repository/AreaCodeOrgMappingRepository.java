package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.AreaCodeOrgMapping;

@Repository
public interface AreaCodeOrgMappingRepository extends JpaRepository<AreaCodeOrgMapping, Long> {

    List<AreaCodeOrgMapping> findByAreaCode(String areaCode);

    List<AreaCodeOrgMapping> findByOrgId(Long orgId);

    @Query("SELECT a FROM AreaCodeOrgMapping a WHERE a.areaCode = :areaCode ORDER BY a.priority ASC")
    List<AreaCodeOrgMapping> findByAreaCodeOrderByPriority(@Param("areaCode") String areaCode);

    Optional<AreaCodeOrgMapping> findFirstByAreaCodeOrderByPriorityAsc(String areaCode);

    boolean existsByAreaCodeAndOrgId(String areaCode, Long orgId);
}

