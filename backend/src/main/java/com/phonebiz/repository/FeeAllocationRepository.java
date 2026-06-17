package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.FeeAllocation;

@Repository
public interface FeeAllocationRepository extends JpaRepository<FeeAllocation, Long> {

    List<FeeAllocation> findByBillMonthAndAllocationLevelOrderByTotalAmountDesc(String billMonth, int level);

    List<FeeAllocation> findByBillMonthAndParentOrgIdAndAllocationLevelOrderByTotalAmountDesc(
            String billMonth, Long parentOrgId, int level);

    Optional<FeeAllocation> findByBillMonthAndAllocationLevelAndOrgId(
            String billMonth, int level, Long orgId);

    boolean existsByBillMonthAndAllocationLevel(String billMonth, int level);

    long countByBillMonthAndAllocationLevel(String billMonth, int level);

    @Modifying
    @Query("DELETE FROM FeeAllocation fa WHERE fa.billMonth = :billMonth AND fa.allocationLevel = :level")
    void deleteByBillMonthAndLevel(@Param("billMonth") String billMonth, @Param("level") int level);

    /** Get distinct parentOrgIds for a given level (used for dropdown) */
    @Query("SELECT DISTINCT fa.parentOrgId FROM FeeAllocation fa WHERE fa.billMonth = :billMonth AND fa.allocationLevel = :level ORDER BY fa.parentOrgId")
    List<Long> findDistinctParentOrgIds(@Param("billMonth") String billMonth, @Param("level") int level);
}
