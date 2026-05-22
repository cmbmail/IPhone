package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    Page<Invoice> findByBillMonth(String billMonth, Pageable pageable);

    Page<Invoice> findByRecipientOrgId(Long orgId, Pageable pageable);

    Page<Invoice> findByStatus(Integer status, Pageable pageable);

    Page<Invoice> findByRecipientOrgIdAndStatus(Long orgId, Integer status, Pageable pageable);

    Page<Invoice> findByBillMonthAndStatus(String billMonth, Integer status, Pageable pageable);


    List<Invoice> findBySourceOrgId(Long orgId);

    @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.billMonth = :billMonth")
    List<Invoice> findPendingForDistribution(@Param("status") Integer status, 
                                             @Param("billMonth") String billMonth);

    long countByStatus(Integer status);

    long countByBillMonth(String billMonth);

    long countByBillMonthAndStatus(String billMonth, Integer status);
}