package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.BillRaw;

@Repository
public interface BillRawRepository extends JpaRepository<BillRaw, Long> {

    List<BillRaw> findByBillMonth(String billMonth);

    Page<BillRaw> findByBillMonth(String billMonth, Pageable pageable);

    List<BillRaw> findByBillMonthAndImportStatus(String billMonth, BillRaw.ImportStatus importStatus);

    Page<BillRaw> findByBillMonthAndImportStatus(String billMonth, BillRaw.ImportStatus importStatus, Pageable pageable);

    List<BillRaw> findByPhoneNumber(String phoneNumber);

    int countByBillMonth(String billMonth);

    int countByBillMonthAndImportStatus(String billMonth, BillRaw.ImportStatus importStatus);

    @Modifying
    @Query("UPDATE BillRaw b SET b.importStatus = :status WHERE b.id = :id")
    void updateImportStatus(@Param("id") Long id, @Param("status") BillRaw.ImportStatus status);

    @Modifying
    @Query("UPDATE BillRaw b SET b.importStatus = 'processed' WHERE b.billMonth = :billMonth AND b.importStatus = 'pending'")
    void markAllAsProcessedByBillMonth(@Param("billMonth") String billMonth);

    List<BillRaw> findByBillMonthAndChargeType(String billMonth, String chargeType);

    Page<BillRaw> findByBillMonthAndChargeType(String billMonth, String chargeType, Pageable pageable);

    List<BillRaw> findByChargeType(String chargeType);

    Page<BillRaw> findByChargeType(String chargeType, Pageable pageable);

    int countByBillMonthAndChargeType(String billMonth, String chargeType);

    int countByChargeType(String chargeType);
}