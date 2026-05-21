package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.InvoiceFile;

@Repository
public interface InvoiceFileRepository extends JpaRepository<InvoiceFile, Long> {

    List<InvoiceFile> findByInvoiceId(Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);
}