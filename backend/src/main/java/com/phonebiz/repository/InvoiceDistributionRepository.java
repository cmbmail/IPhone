package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.InvoiceDistribution;

@Repository
public interface InvoiceDistributionRepository extends JpaRepository<InvoiceDistribution, Long> {

    List<InvoiceDistribution> findByInvoiceId(Long invoiceId);

    List<InvoiceDistribution> findByRecipientUser(String recipientUser);

    long countByInvoiceIdAndDistributionStatus(Long invoiceId, InvoiceDistribution.DistributionStatus status);
}