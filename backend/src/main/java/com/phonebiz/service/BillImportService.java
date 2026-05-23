package com.phonebiz.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.entity.BillRaw;
import com.phonebiz.repository.BillRawRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillImportService {

    private final BillRawRepository billRawRepository;
    private final BillAllocationService billAllocationService;
    private final ObjectMapper objectMapper;

    private final Map<String, String> allocationStatusMap = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> SKIP_VALUES = Set.of("合计", "小计", "总计", "sum", "total");

    @Transactional
    public int importBillRaw(String billMonth, MultipartFile file, String operator) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.SYS_001, "Only .xlsx files are allowed");
        }
        // Validate file magic bytes (ZIP header for xlsx: PK\x03\x04)
        byte[] fileBytes = file.getBytes();
        if (fileBytes.length < 4 || fileBytes[0] != 0x50 || fileBytes[1] != 0x4B || fileBytes[2] != 0x03 || fileBytes[3] != 0x04) {
            throw new BusinessException(ErrorCode.SYS_001, "Invalid file format, expected xlsx");
        }

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(fileBytes))) {
            int total = 0;
            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                Integer chargeType = resolveChargeType(sheetName);
                log.info("Processing sheet '{}' as chargeType={}", sheetName, chargeType);

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                List<BillRaw> batch = new ArrayList<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isSkipRow(row)) continue;

                    try {
                        BillRaw bill = buildBillRaw(row, billMonth, chargeType, fileName, operator);
                        batch.add(bill);
                        if (batch.size() >= 100) {
                            billRawRepository.saveAll(batch);
                            total += batch.size();
                            batch.clear();
                        }
                    } catch (Exception e) {
                        log.warn("Skip row {} in sheet '{}': {}", i, sheetName, e.getMessage());
                    }
                }
                if (!batch.isEmpty()) {
                    billRawRepository.saveAll(batch);
                    total += batch.size();
                }
            }
            log.info("Imported {} records from {}", total, fileName);
            return total;
        }
    }

    private Integer resolveChargeType(String sheetName) {
        if (sheetName == null) return BillRaw.CHARGE_TYPE_PHONE;
        String s = sheetName.toLowerCase();
        if (s.contains("录音") || s.contains("recording")) return BillRaw.CHARGE_TYPE_RECORDING;
        if (s.contains("彩铃") || s.contains("ringtone")) return BillRaw.CHARGE_TYPE_RINGTONE;
        if (s.contains("闪信") || s.contains("flash") || s.contains("sms") || s.contains("来电名片") || s.contains("名片") || s.contains("明细")) return BillRaw.CHARGE_TYPE_FLASH_SMS;
        return BillRaw.CHARGE_TYPE_PHONE;
    }

    private boolean isSkipRow(Row row) {
        Cell first = row.getCell(0);
        if (first == null) return true;
        String v = cellStr(first).trim();
        if (v.isEmpty()) return true;
        if (SKIP_VALUES.contains(v)) return true;
        // Skip rows where first cell is not a valid identifier (e.g. AIGC watermarks)
        if (!Character.isDigit(v.charAt(0)) && v.charAt(0) != '+') return true;
        return false;
    }

    private BillRaw buildBillRaw(Row row, String billMonth, Integer chargeType, String fileName, String operator) {
        BillRaw b = new BillRaw();
        b.setBillMonth(billMonth);
        b.setChargeType(chargeType);
        b.setFileName(fileName);
        b.setImportedBy(operator);
        b.setImportStatus(BillRaw.IMPORT_PENDING);
        b.setChargeAmount(BigDecimal.ZERO);  // default, overwritten by fillXxx if applicable

        int cols = row.getLastCellNum();
        String[] vals = new String[cols];
        for (int i = 0; i < cols; i++) {
            vals[i] = cellStr(row.getCell(i));
        }

        switch (chargeType) {
            case 0 -> fillPhone(b, vals);
            case 1 -> fillRecording(b, vals);
            case 2 -> fillRingtone(b, vals);
            case 3 -> fillFlashSms(b, vals);
            default -> fillPhone(b, vals);
        }

        try {
            b.setRawData(objectMapper.writeValueAsString(Map.of()));
        } catch (JsonProcessingException ignored) {}

        return b;
    }


    private void fillPhone(BillRaw b, String[] v) {
        // 按号码费用: 号码(0) 分配时间(1) 用户ID(2) 部门(3) 平台使用费(4) 码号月租费(5)
        //            外呼时长(6) 转接外呼时长(7) 国内费用(8) 国际时长(9) 国际费用(10) 费用小计(11) 备注(12)
        b.setPhoneNumber(s(v, 0));
        b.setAllocationTime(parseDate(s(v, 1)));
        b.setEmployeeNo(s(v, 2));
        b.setDeptName(s(v, 3));
        b.setPlatformUsageFee(dec(s(v, 4)));
        b.setNumberMonthlyRent(dec(s(v, 5)));
        b.setOutboundDuration(intg(s(v, 6)));
        b.setTransferOutboundDuration(intg(s(v, 7)));
        b.setDomesticCharge(dec(s(v, 8)));
        b.setInternationalDuration(intg(s(v, 9)));
        b.setInternationalCharge(dec(s(v, 10)));
        b.setChargeAmount(dec(s(v, 11)));
        b.setRemark(s(v, 12));
    }

    private void fillRecording(BillRaw b, String[] v) {
        // 录音费用: 分机号(0) 号码(1) 开启时间(2) 关闭时间(3) 开始时间(4) 结束时间(5) 天数(6) 费用小计(7)
        b.setExtensionNumber(s(v, 0));
        b.setPhoneNumber(s(v, 1));  // 子号码存入phone_number
        b.setActivationTime(parseDate(s(v, 2)));
        b.setClosingTime(s(v, 3));
        b.setBillingStartDate(parseDate(s(v, 4)));
        b.setBillingEndDate(parseDate(s(v, 5)));
        b.setDays(intg(s(v, 6)));
        b.setChargeAmount(dec(s(v, 7)));
    }

    private void fillRingtone(BillRaw b, String[] v) {
        // 彩铃费用: 分机号(0) 号码(1) 开通时间(2) 归属(3) 费用(4)
        b.setExtensionNumber(s(v, 0));
        b.setPhoneNumber(s(v, 1));  // 子号码存入phone_number
        b.setActivationTime(parseDate(s(v, 2)));
        b.setDeptName(s(v, 3));
        b.setChargeAmount(dec(s(v, 4)));
    }

    private void fillFlashSms(BillRaw b, String[] v) {
        // 闪信费用: 月份(0) 主号码(1) 子号码(2) 地市(3) 下发量(4)
        // Note: billMonth already set by buildBillRaw from parameter; Excel col0 is just display
        // b.setBillMonth(s(v, 0));  // keep parameter billMonth (e.g. 2026-04) not Excel (e.g. 202601)
        b.setPhoneNumber(s(v, 1));  // 子号码存入phone_number
        b.setCity(s(v, 3));
        b.setSendCount(intg(s(v, 4)));
        b.setChargeAmount(BigDecimal.ZERO);  // flash SMS has no per-row charge
    }

    // --- helpers ---

    private String s(String[] v, int i) {
        return (i >= 0 && i < v.length && v[i] != null) ? v[i].trim() : "";
    }

    private BigDecimal dec(String val) {
        if (val == null || val.isEmpty() || val.equalsIgnoreCase("nan")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer intg(String val) {
        if (val == null || val.isEmpty() || val.equalsIgnoreCase("nan")) return null;
        try {
            return (int) Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isEmpty()) return null;
        String t = val.trim();
        // try yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(t, DATETIME_FMT).toLocalDate();
        } catch (Exception ignored) {}
        // try yyyy-MM-dd
        try {
            return LocalDate.parse(t, DATE_FMT);
        } catch (Exception ignored) {}
        // try Excel numeric date
        try {
            double d = Double.parseDouble(t);
            return LocalDate.of(1899, 12, 30).plusDays((long) d);
        } catch (Exception ignored) {}
        return null;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getStringCellValue()); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    @org.springframework.scheduling.annotation.Async("importTaskExecutor")
    public void processImportAsync(String billMonth, String operator) {
        allocationStatusMap.put(billMonth, "processing");
        try {
            processAndAllocate(billMonth, operator);
            allocationStatusMap.put(billMonth, "completed");
        } catch (Exception e) {
            log.error("Bill import and allocation failed for month {}: {}", billMonth, e.getMessage(), e);
            allocationStatusMap.put(billMonth, "failed:" + e.getMessage());
        }
    }

    public String getAllocationStatus(String billMonth) {
        return allocationStatusMap.getOrDefault(billMonth, "not_found");
    }

    public void clearAllocationStatus(String billMonth) {
        allocationStatusMap.remove(billMonth);
    }

    @Transactional
    public void processAndAllocate(String billMonth, String operator) {
        log.info("Starting bill allocation process for month {}", billMonth);
        List<BillRaw> pendingBills = billRawRepository.findByBillMonthAndImportStatus(billMonth, BillRaw.IMPORT_PENDING);
        if (pendingBills.isEmpty()) {
            log.info("No pending bills to process for month {}", billMonth);
            return;
        }
        billAllocationService.autoAllocateAndSave(pendingBills, billMonth, operator);
        for (BillRaw bill : pendingBills) {
            bill.setImportStatus(BillRaw.IMPORT_PROCESSED);
        }
        billRawRepository.saveAll(pendingBills);
        log.info("Bill allocation completed for month {}: {} records processed", billMonth, pendingBills.size());
    }
}
