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

    List<PhoneSnapshot> findBySnapshotMonthAndStatus(String snapshotMonth, String status);

    @Query("SELECT DISTINCT p.snapshotMonth FROM PhoneSnapshot p ORDER BY p.snapshotMonth DESC")
    List<String> findDistinctSnapshotMonths();

    @Query("SELECT p FROM PhoneSnapshot p WHERE p.snapshotMonth = :snapshotMonth AND p.orgId IN :orgIds")
    List<PhoneSnapshot> findBySnapshotMonthAndOrgIds(@Param("snapshotMonth") String snapshotMonth, @Param("orgIds") List<Long> orgIds);

    Page<PhoneSnapshot> findBySnapshotMonth(String snapshotMonth, Pageable pageable);

    int countBySnapshotMonth(String snapshotMonth);

    int countBySnapshotMonthAndStatus(String snapshotMonth, String status);

    int countBySnapshotMonthAndOrgId(String snapshotMonth, Long orgId);

    void deleteBySnapshotMonth(String snapshotMonth);
}