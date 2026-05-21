package com.phonebiz.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.Invoice;
import com.phonebiz.entity.InvoiceDistribution;
import com.phonebiz.entity.InvoiceFile;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.InvoiceDistributionRepository;
import com.phonebiz.repository.InvoiceFileRepository;
import com.phonebiz.repository.InvoiceRepository;
import com.phonebiz.repository.OrgStructureRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceFileRepository invoiceFileRepository;
    private final InvoiceDistributionRepository invoiceDistributionRepository;
    private final OrgStructureRepository orgStructureRepository;
    private final NotificationService notificationService;

    private static final String INVOICE_STORAGE_PATH = "/data/invoices/";
    private static final Pattern INVOICE_NO_PATTERN = Pattern.compile("[A-Za-z0-9]{8,20}");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d{1,10}(?:\\.\\d{1,2})?)");
    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,50}");

    private static final Pattern BILL_MONTH_PATTERN = Pattern.compile("^\\d{6}$");

    @Transactional
    public Invoice uploadInvoice(MultipartFile file, String billMonth, Long sourceOrgId, String operator) throws IOException {
        if (!BILL_MONTH_PATTERN.matcher(billMonth).matches()) {
            throw new BusinessException(ErrorCode.SYS_002, "Invalid billMonth format, expected yyyyMM");
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || (!originalFileName.endsWith(".pdf") && !originalFileName.endsWith(".PDF"))) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        String invoiceNo = extractInvoiceNo(originalFileName);
        if (invoiceRepository.findByInvoiceNo(invoiceNo).isPresent()) {
            throw new BusinessException(ErrorCode.SYS_001);
        }

        String storagePath = ensureStoragePath(billMonth);
        String storedFileName = generateStoredFileName(invoiceNo, originalFileName);
        Path filePath = Paths.get(storagePath, storedFileName);
        Files.copy(file.getInputStream(), filePath);

        String md5 = calculateMD5(file.getBytes());

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .billMonth(billMonth)
                .sourceOrgId(sourceOrgId)
                .sourceOrgName(getSourceOrgName(sourceOrgId))
                .recipientOrgId(sourceOrgId)
                .build();

        invoice = invoiceRepository.save(invoice);

        InvoiceFile invoiceFile = InvoiceFile.builder()
                .invoiceId(invoice.getId())
                .fileName(originalFileName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .md5(md5)
                .build();
        invoiceFileRepository.save(invoiceFile);

        processOcrAndDistribute(invoice.getId(), filePath);

        log.info("Invoice uploaded: invoiceNo={}, billMonth={}, file={}", invoiceNo, billMonth, originalFileName);
        return invoice;
    }

    private String extractInvoiceNo(String fileName) {
        Matcher matcher = INVOICE_NO_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String ensureStoragePath(String billMonth) throws IOException {
        if (!BILL_MONTH_PATTERN.matcher(billMonth).matches()) {
            throw new BusinessException(ErrorCode.SYS_002, "Invalid billMonth format");
        }
        Path path = Paths.get(INVOICE_STORAGE_PATH, billMonth).normalize();
        if (!path.startsWith(INVOICE_STORAGE_PATH)) {
            throw new BusinessException(ErrorCode.SYS_002, "Invalid storage path");
        }
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path.toString();
    }

    private String generateStoredFileName(String invoiceNo, String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return invoiceNo + extension;
    }

    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String getSourceOrgName(Long orgId) {
        return orgStructureRepository.findById(orgId)
                .map(OrgStructure::getName)
                .orElse("未知公司");
    }

    private void processOcrAndDistribute(Long invoiceId, Path filePath) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();

        String ocrText = performOcrSimulation(filePath.getFileName().toString());
        invoice.setOcrText(ocrText);
        invoice.setOcrConfidence(new BigDecimal("0.85"));

        String companyName = extractCompanyName(ocrText);
        if (companyName != null && !companyName.isEmpty()) {
            invoice.setSourceOrgName(companyName);
            Long recipientOrgId = matchRecipientOrg(companyName);
            if (recipientOrgId != null) {
                invoice.setRecipientOrgId(recipientOrgId);
            }
        }

        extractAmountFromOcr(ocrText, invoice);

        invoice.setStatus(Invoice.InvoiceStatus.distributed);
        invoice.setDistributeAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        distributeToRecipient(invoice);
    }

    private String performOcrSimulation(String fileName) {
        return """
                发票号码: FP2024120001
                开票日期: 2024-12-15
                购买方名称: 子公司A
                销售方名称: 电信运营商
                金额: 12580.00
                税额: 1635.40
                价税合计: 14215.40
                """;
    }

    private String extractCompanyName(String ocrText) {
        Matcher matcher = COMPANY_NAME_PATTERN.matcher(ocrText);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group());
        }
        for (String name : names) {
            if (name.contains("公司") || name.contains("集团") || name.contains("子")) {
                return name;
            }
        }
        return names.isEmpty() ? null : names.get(0);
    }

    private Long matchRecipientOrg(String companyName) {
        return orgStructureRepository.findAll().stream()
                .filter(org -> org.getName().contains(companyName) || companyName.contains(org.getName()))
                .map(org -> org.getId())
                .findFirst()
                .orElse(null);
    }

    private void extractAmountFromOcr(String ocrText, Invoice invoice) {
        Matcher matcher = AMOUNT_PATTERN.matcher(ocrText);
        List<BigDecimal> amounts = new ArrayList<>();
        while (matcher.find()) {
            try {
                amounts.add(new BigDecimal(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (!amounts.isEmpty()) {
            amounts.sort(Collections.reverseOrder());
            invoice.setAmount(amounts.get(0));
            if (amounts.size() > 1) {
                invoice.setTaxAmount(amounts.get(1));
            }
        }
    }

    private void distributeToRecipient(Invoice invoice) {
        String recipientUser = findRecipientUser(invoice.getRecipientOrgId());

        InvoiceDistribution distribution = InvoiceDistribution.builder()
                .invoiceId(invoice.getId())
                .recipientUser(recipientUser)
                .distributionStatus(InvoiceDistribution.DistributionStatus.success)
                .build();
        invoiceDistributionRepository.save(distribution);

        notificationService.createNotification(
                1L,
                com.phonebiz.entity.Notification.NotificationType.SYSTEM_ALERT,
                "发票已分发",
                String.format("发票 %s 已分发至您的组织", invoice.getInvoiceNo()),
                invoice.getId(),
                "Invoice"
        );

        log.info("Invoice distributed: invoiceNo={}, recipient={}", invoice.getInvoiceNo(), recipientUser);
    }

    private String findRecipientUser(Long orgId) {
        return "finance_" + orgId;
    }

    @Transactional
    public Invoice confirmInvoice(Long invoiceId, String operator) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        invoice.setStatus(Invoice.InvoiceStatus.confirmed);
        invoice.setConfirmedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markAsRead(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        if (invoice.getReadAt() == null) {
            invoice.setReadAt(LocalDateTime.now());
            invoice.setStatus(Invoice.InvoiceStatus.read);
            return invoiceRepository.save(invoice);
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesByOrg(Long orgId, Pageable pageable) {
        return invoiceRepository.findByRecipientOrgId(orgId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesByBillMonth(String billMonth, Pageable pageable) {
        return invoiceRepository.findByBillMonth(billMonth, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));
    }

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));

        List<InvoiceFile> files = invoiceFileRepository.findByInvoiceId(id);
        for (InvoiceFile file : files) {
            try {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete invoice file: {}", file.getFilePath());
            }
        }

        invoiceFileRepository.deleteByInvoiceId(id);
        invoiceDistributionRepository.deleteAll(invoiceDistributionRepository.findByInvoiceId(id));
        invoiceRepository.delete(invoice);

        log.info("Invoice deleted: id={}, invoiceNo={}", id, invoice.getInvoiceNo());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getInvoiceStatistics(String billMonth) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", invoiceRepository.countByBillMonth(billMonth));
        stats.put("pending", invoiceRepository.countByBillMonthAndStatus(billMonth, Invoice.InvoiceStatus.pending));
        stats.put("distributed", invoiceRepository.countByBillMonthAndStatus(billMonth, Invoice.InvoiceStatus.distributed));
        stats.put("read", invoiceRepository.countByBillMonthAndStatus(billMonth, Invoice.InvoiceStatus.read));
        stats.put("confirmed", invoiceRepository.countByBillMonthAndStatus(billMonth, Invoice.InvoiceStatus.confirmed));
        return stats;
    }
}