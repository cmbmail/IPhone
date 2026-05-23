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

    List<BillRaw> findByBillMonthAndImportStatus(String billMonth, Integer importStatus);

    Page<BillRaw> findByBillMonthAndImportStatus(String billMonth, Integer importStatus, Pageable pageable);

    List<BillRaw> findByPhoneNumber(String phoneNumber);

    int countByBillMonth(String billMonth);

    int countByBillMonthAndImportStatus(String billMonth, Integer importStatus);

    @Modifying
    @Query("UPDATE BillRaw b SET b.importStatus = :status WHERE b.id = :id")
    void updateImportStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE BillRaw b SET b.importStatus = 1 WHERE b.billMonth = :billMonth AND b.importStatus = 0")
    void markAllAsProcessedByBillMonth(@Param("billMonth") String billMonth);

    List<BillRaw> findByBillMonthAndChargeType(String billMonth, Integer chargeType);

    Page<BillRaw> findByBillMonthAndChargeType(String billMonth, Integer chargeType, Pageable pageable);

    List<BillRaw> findByChargeType(Integer chargeType);

    Page<BillRaw> findByChargeType(Integer chargeType, Pageable pageable);

    int countByBillMonthAndChargeType(String billMonth, Integer chargeType);

    int countByChargeType(Integer chargeType);

    // Summary by branch (level-1 org) for a given bill month
    // Joins bill_raw.department -> org_structure.name to find the level-1 ancestor branch
    // Uses path like /1/2/5 to extract the level-1 org id (2nd segment)
    @Query(value = """
        SELECT COALESCE(l1.branch_name, r.dept_name, '未分配') AS branch_name,
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.platform_usage_fee ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.number_monthly_rent ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.outbound_duration ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.transfer_outbound_duration ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.domestic_charge ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'PHONE' THEN r.international_charge ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'RECORDING' THEN r.charge_amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'RINGTONE' THEN r.charge_amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN r.charge_type = 'FLASH_SMS' THEN r.charge_amount ELSE 0 END), 0),
               COALESCE(SUM(r.charge_amount), 0)
        FROM bill_raw r
        LEFT JOIN org_structure dept_org ON r.dept_name = dept_org.name
        LEFT JOIN org_structure l1 ON l1.level = 1
            AND l1.id = CASE
                WHEN dept_org.path REGEXP '^/[0-9]+/[0-9]+' 
                THEN CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(dept_org.path, '/', 3), '/', -1) AS UNSIGNED)
                ELSE NULL END
        WHERE r.bill_month = :billMonth
        GROUP BY COALESCE(l1.branch_name, r.dept_name, '未分配')
        ORDER BY branch_name
        """, nativeQuery = true)
    List<Object[]> sumByBranch(@Param("billMonth") String billMonth);

}
