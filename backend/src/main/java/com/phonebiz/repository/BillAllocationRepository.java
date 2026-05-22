package com.phonebiz.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.BillAllocation;

@Repository
public interface BillAllocationRepository extends JpaRepository<BillAllocation, Long> {

    List<BillAllocation> findByBillMonth(String billMonth);

    Page<BillAllocation> findByBillMonth(String billMonth, Pageable pageable);

    List<BillAllocation> findByBillMonthAndSnapshotOrgId(String billMonth, Long orgId);

    Page<BillAllocation> findByBillMonthAndSnapshotOrgId(String billMonth, Long orgId, Pageable pageable);

    List<BillAllocation> findByBillMonthAndCostCenterCode(String billMonth, String costCenterCode);

    Page<BillAllocation> findByBillMonthAndAnomalyFlag(String billMonth, Boolean anomalyFlag, Pageable pageable);

    Page<BillAllocation> findByBillMonthAndAdminConfirmOrg(String billMonth, BillAllocation.ConfirmStatus status, Pageable pageable);

    Page<BillAllocation> findByBillMonthAndAdminConfirmAmount(String billMonth, BillAllocation.ConfirmStatus status, Pageable pageable);

    Page<BillAllocation> findByBillMonthAndFinanceConfirmAnomaly(String billMonth, BillAllocation.FinanceConfirmStatus status, Pageable pageable);

    Page<BillAllocation> findByBillMonthAndFinanceConfirmSubmit(String billMonth, BillAllocation.FinanceSubmitStatus status, Pageable pageable);

    int countByBillMonth(String billMonth);

    int countByBillMonthAndAnomalyFlag(String billMonth, Boolean anomalyFlag);

    int countByBillMonthAndAdminConfirmOrg(String billMonth, BillAllocation.ConfirmStatus status);

    int countByBillMonthAndAdminConfirmAmount(String billMonth, BillAllocation.ConfirmStatus status);

    int countByBillMonthAndFinanceConfirmAnomaly(String billMonth, BillAllocation.FinanceConfirmStatus status);

    int countByBillMonthAndFinanceConfirmSubmit(String billMonth, BillAllocation.FinanceSubmitStatus status);

    @Query("SELECT SUM(b.chargeAmount) FROM BillAllocation b WHERE b.billMonth = :billMonth GROUP BY b.snapshotOrgId")
    List<BigDecimal> sumChargeAmountByOrgId(@Param("billMonth") String billMonth);

    @Query("SELECT b.snapshotOrgId, SUM(b.chargeAmount) FROM BillAllocation b WHERE b.billMonth = :billMonth GROUP BY b.snapshotOrgId")
    List<Object[]> sumChargeAmountGroupByOrgId(@Param("billMonth") String billMonth);

    @Query("SELECT b.costCenterCode, SUM(b.chargeAmount) FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.costCenterCode IS NOT NULL GROUP BY b.costCenterCode")
    List<Object[]> sumChargeAmountGroupByCostCenter(@Param("billMonth") String billMonth);

    @Modifying
    @Query("UPDATE BillAllocation b SET b.adminConfirmOrg = :status, b.adminConfirmBy = :confirmBy, b.adminConfirmAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    void updateAdminConfirmOrg(@Param("id") Long id, @Param("status") BillAllocation.ConfirmStatus status, @Param("confirmBy") String confirmBy);

    @Modifying
    @Query("UPDATE BillAllocation b SET b.adminConfirmAmount = :status, b.adminConfirmBy = :confirmBy, b.adminConfirmAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    void updateAdminConfirmAmount(@Param("id") Long id, @Param("status") BillAllocation.ConfirmStatus status, @Param("confirmBy") String confirmBy);

    @Modifying
    @Query("UPDATE BillAllocation b SET b.financeConfirmAnomaly = :status, b.financeConfirmBy = :confirmBy, b.financeConfirmAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    void updateFinanceConfirmAnomaly(@Param("id") Long id, @Param("status") BillAllocation.FinanceConfirmStatus status, @Param("confirmBy") String confirmBy);

    @Modifying
    @Query("UPDATE BillAllocation b SET b.financeConfirmSubmit = :status, b.financeSubmitBy = :submitBy, b.financeSubmitAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    void updateFinanceConfirmSubmit(@Param("id") Long id, @Param("status") BillAllocation.FinanceSubmitStatus status, @Param("submitBy") String submitBy);

    @Query("SELECT b.snapshotOrgId, COUNT(b), SUM(b.chargeAmount) FROM BillAllocation b WHERE b.billMonth = :billMonth GROUP BY b.snapshotOrgId")
    List<Object[]> getAllocationSummaryByOrg(@Param("billMonth") String billMonth);


    // Report aggregation queries - avoid loading 80k+ entities into memory
    @Query("SELECT COALESCE(SUM(b.chargeAmount), 0) FROM BillAllocation b WHERE b.billMonth = :billMonth")
    BigDecimal sumChargeAmountByMonth(@Param("billMonth") String billMonth);

    @Query("SELECT COALESCE(SUM(b.chargeAmount), 0) FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.anomalyFlag = true")
    BigDecimal sumAnomalyAmountByMonth(@Param("billMonth") String billMonth);

    @Query("SELECT b.adminConfirmOrg, COUNT(b) FROM BillAllocation b WHERE b.billMonth = :billMonth GROUP BY b.adminConfirmOrg")
    List<Object[]> countByConfirmStatusGroupBy(@Param("billMonth") String billMonth);

    @Query("SELECT b.snapshotOrgId, SUM(b.chargeAmount), COUNT(b) FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.snapshotOrgId IS NOT NULL GROUP BY b.snapshotOrgId")
    List<Object[]> sumAndCountGroupByOrgId(@Param("billMonth") String billMonth);

    @Query("SELECT b.anomalyReason, COUNT(b) FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.anomalyFlag = true AND b.anomalyReason IS NOT NULL GROUP BY b.anomalyReason")
    List<Object[]> countAnomalyByReason(@Param("billMonth") String billMonth);

    @Query("SELECT COALESCE(SUM(b.chargeAmount), 0) FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.anomalyFlag = true")
    BigDecimal sumAnomalyChargeByMonth(@Param("billMonth") String billMonth);

    @Query("SELECT b.id, b.phoneNumber, b.chargeAmount, b.anomalyReason, b.snapshotOrgName, b.createdAt FROM BillAllocation b WHERE b.billMonth = :billMonth AND b.anomalyFlag = true ORDER BY b.chargeAmount DESC")
    List<Object[]> findAnomalyProjectionByMonth(@Param("billMonth") String billMonth);

    List<BillAllocation> findByPhoneNumber(String phoneNumber);
}

