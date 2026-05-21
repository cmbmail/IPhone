package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.SubsidiaryReconciliation;

@Repository
public interface SubsidiaryReconciliationRepository extends JpaRepository<SubsidiaryReconciliation, Long> {

    List<SubsidiaryReconciliation> findByBillMonth(String billMonth);

    Page<SubsidiaryReconciliation> findByBillMonth(String billMonth, Pageable pageable);

    Optional<SubsidiaryReconciliation> findByBillMonthAndSubsidiaryOrgId(String billMonth, Long subsidiaryOrgId);

    List<SubsidiaryReconciliation> findByBillMonthAndReconciliationStatus(String billMonth, SubsidiaryReconciliation.ReconciliationStatus status);

    @Modifying
    @Query("UPDATE SubsidiaryReconciliation r SET r.reconciliationStatus = :status, r.subsidiaryConfirmBy = :confirmBy, r.subsidiaryConfirmAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    void confirmBySubsidiary(@Param("id") Long id, @Param("status") SubsidiaryReconciliation.ReconciliationStatus status, @Param("confirmBy") String confirmBy);

    @Modifying
    @Query("UPDATE SubsidiaryReconciliation r SET r.reconciliationStatus = :status, r.groupConfirmBy = :confirmBy, r.groupConfirmAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    void confirmByGroup(@Param("id") Long id, @Param("status") SubsidiaryReconciliation.ReconciliationStatus status, @Param("confirmBy") String confirmBy);

    int countByBillMonth(String billMonth);

    int countByBillMonthAndReconciliationStatus(String billMonth, SubsidiaryReconciliation.ReconciliationStatus status);
}

