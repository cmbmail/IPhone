package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneSnapshot;

@Repository
public interface PhoneSnapshotRepository extends JpaRepository<PhoneSnapshot, Long> {

    Optional<PhoneSnapshot> findBySnapshotMonthAndPhoneId(String snapshotMonth, Long phoneId);

    boolean existsBySnapshotMonthAndPhoneId(String snapshotMonth, Long phoneId);

    List<PhoneSnapshot> findBySnapshotMonth(String snapshotMonth);

    List<PhoneSnapshot> findBySnapshotMonthAndOrgId(String snapshotMonth, Long orgId);

    List<PhoneSnapshot> findBySnapshotMonthAndCostCenterCode(String snapshotMonth, String costCenterCode);

    List<PhoneSnapshot> findBySnapshotMonthAndStatus(String snapshotMonth, Integer status);

    @Query("SELECT DISTINCT p.snapshotMonth FROM PhoneSnapshot p ORDER BY p.snapshotMonth DESC")
    List<String> findDistinctSnapshotMonths();

    @Query("SELECT p FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND p.orgId IN :orgIds")
    List<PhoneSnapshot> findBySnapshotMonthAndOrgIds(@Param("snapshotMonth") String snapshotMonth, @Param("orgIds") List<Long> orgIds);

    // Paged queries
    Page<PhoneSnapshot> findBySnapshotMonth(String snapshotMonth, Pageable pageable);

    Page<PhoneSnapshot> findBySnapshotMonthAndStatus(String snapshotMonth, Integer status, Pageable pageable);

    Page<PhoneSnapshot> findBySnapshotMonthAndOrgId(String snapshotMonth, Long orgId, Pageable pageable);

    Page<PhoneSnapshot> findBySnapshotMonthAndBranchOrgId(String snapshotMonth, Long branchOrgId, Pageable pageable);

    // By billMonth
    List<PhoneSnapshot> findByBillMonth(String billMonth);

    Page<PhoneSnapshot> findByBillMonth(String billMonth, Pageable pageable);

    // Count queries
    int countBySnapshotMonth(String snapshotMonth);

    int countBySnapshotMonthAndStatus(String snapshotMonth, Integer status);

    int countBySnapshotMonthAndOrgId(String snapshotMonth, Long orgId);

    long countByBillMonth(String billMonth);

    // Aggregation queries
    @Query("SELECT p.status, COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth GROUP BY p.status")
    List<Object[]> countGroupByStatus(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT p.orgId, COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND p.orgId IS NOT NULL GROUP BY p.orgId")
    List<Object[]> countGroupByOrgId(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT p.branchOrgId, COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND p.branchOrgId IS NOT NULL GROUP BY p.branchOrgId")
    List<Object[]> countGroupByBranchOrgId(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT p.allocationStatus, COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth GROUP BY p.allocationStatus")
    List<Object[]> countGroupByAllocationStatus(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth")
    long countByMonth(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND p.employeeNo IS NOT NULL AND p.employeeNo <> ''")
    long countAllocatedByMonth(@Param("snapshotMonth") String snapshotMonth);

    @Query("SELECT COUNT(p) FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND (p.employeeNo IS NULL OR p.employeeNo = '')")
    long countIdleByMonth(@Param("snapshotMonth") String snapshotMonth);

    void deleteBySnapshotMonth(String snapshotMonth);
}
