package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.PhoneImportRequest;
import com.phonebiz.entity.ImportBatch;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.ImportBatchRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportBatchRepository batchRepository;
    private final PhoneNumberRepository phoneRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, Integer> progressMap = new ConcurrentHashMap<>();

    @Transactional
    public ImportBatch createBatch(int totalCount, String operator) {
        String batchId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        
        ImportBatch batch = ImportBatch.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .successCount(0)
                .failCount(0)
                .status(ImportBatch.ImportStatus.PENDING)
                .operator(operator)
                .build();
        
        return batchRepository.save(batch);
    }

    @Async("importTaskExecutor")
    public void processImportAsync(String batchId, List<Map<String, Object>> dataList, 
                                   PhoneImportRequest.ConflictStrategy strategy, String operator) {
        try {
            processImport(batchId, dataList, strategy, operator);
        } catch (Exception e) {
            log.error("Import batch {} failed: {}", batchId, e.getMessage(), e);
            updateBatchStatus(batchId, ImportBatch.ImportStatus.FAILED, e.getMessage());
        }
    }

    @Transactional
    public void processImport(String batchId, List<Map<String, Object>> dataList,
                              PhoneImportRequest.ConflictStrategy strategy, String operator) {
        ImportBatch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_001));

        batch.setStatus(ImportBatch.ImportStatus.PROCESSING);
        batchRepository.save(batch);

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int batchSize = 100;

        Map<String, PhoneNumber> existingPhones = loadExistingPhones();

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<Map<String, Object>> batchData = dataList.subList(i, end);
            
            List<PhoneNumber> toSave = new ArrayList<>();
            
            for (Map<String, Object> data : batchData) {
                try {
                    PhoneNumber phone = parseAndValidate(data, existingPhones, strategy, errors);
                    if (phone != null) {
                        toSave.add(phone);
                    }
                } catch (Exception e) {
                    errors.add("Row " + (i + toSave.size() + 1) + ": " + e.getMessage());
                    failCount++;
                }
            }

            if (!toSave.isEmpty()) {
                phoneRepository.saveAll(toSave);
                successCount += toSave.size();
                
                for (PhoneNumber phone : toSave) {
                    existingPhones.put(phone.getPhoneNumber(), phone);
                }
            }

            progressMap.put(batchId, (int) ((i + batchSize) * 100.0 / dataList.size()));
        }

        batch.setSuccessCount(successCount);
        batch.setFailCount(failCount);
        
        if (failCount == 0) {
            batch.setStatus(ImportBatch.ImportStatus.COMPLETED);
        } else {
            batch.setStatus(ImportBatch.ImportStatus.COMPLETED);
            try {
                batch.setErrorDetail(objectMapper.writeValueAsString(errors));
            } catch (JsonProcessingException e) {
                batch.setErrorDetail(errors.toString());
            }
        }
        
        batchRepository.save(batch);
        progressMap.remove(batchId);
        
        log.info("Import batch {} completed: {}/{} success", batchId, successCount, dataList.size());
    }

    private Map<String, PhoneNumber> loadExistingPhones() {
        Map<String, PhoneNumber> map = new HashMap<>();
        phoneRepository.findAll().forEach(p -> map.put(p.getPhoneNumber(), p));
        return map;
    }

    private PhoneNumber parseAndValidate(Map<String, Object> data, Map<String, PhoneNumber> existingPhones,
                                         PhoneImportRequest.ConflictStrategy strategy, List<String> errors) {
        String phoneNumber = normalizePhoneNumber(String.valueOf(data.get("phoneNumber")));
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            errors.add("Empty phone number");
            return null;
        }

        if (existingPhones.containsKey(phoneNumber)) {
            PhoneNumber existing = existingPhones.get(phoneNumber);
            
            switch (strategy) {
                case ERROR:
                    errors.add("Phone number " + phoneNumber + " already exists");
                    return null;
                case SKIP:
                    return null;
                case OVERWRITE:
                    if (existing.getStatus() != PhoneNumber.PhoneStatus.idle) {
                        return null;
                    }
                    return updateExistingPhone(existing, data);
                default:
                    return null;
            }
        }

        return createNewPhone(data, phoneNumber);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private PhoneNumber createNewPhone(Map<String, Object> data, String phoneNumber) {
        PhoneNumber phone = new PhoneNumber();
        phone.setPhoneNumber(phoneNumber);
        phone.setStatus(PhoneNumber.PhoneStatus.idle);
        
        if (data.containsKey("orgId")) {
            try {
                phone.setOrgId(Long.parseLong(String.valueOf(data.get("orgId"))));
            } catch (NumberFormatException e) {
                phone.setOrgId(null);
            }
        }
        
        if (data.containsKey("extensionNumber")) {
            phone.setExtensionNumber(String.valueOf(data.get("extensionNumber")));
        }
        
        if (data.containsKey("remark")) {
            phone.setRemark(String.valueOf(data.get("remark")));
        }
        
        phone.setCreatedAt(LocalDateTime.now());
        phone.setUpdatedAt(LocalDateTime.now());
        
        return phone;
    }

    private PhoneNumber updateExistingPhone(PhoneNumber existing, Map<String, Object> data) {
        if (data.containsKey("orgId")) {
            try {
                existing.setOrgId(Long.parseLong(String.valueOf(data.get("orgId"))));
            } catch (NumberFormatException ignored) {}
        }
        
        if (data.containsKey("extensionNumber")) {
            existing.setExtensionNumber(String.valueOf(data.get("extensionNumber")));
        }
        
        if (data.containsKey("remark")) {
            existing.setRemark(String.valueOf(data.get("remark")));
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        return existing;
    }

    @Transactional(readOnly = true)
    public ImportBatch getBatchStatus(String batchId) {
        return batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_001));
    }

    public int getProgress(String batchId) {
        return progressMap.getOrDefault(batchId, 0);
    }

    private void updateBatchStatus(String batchId, ImportBatch.ImportStatus status, String errorDetail) {
        try {
            batchRepository.findByBatchId(batchId).ifPresent(batch -> {
                batch.setStatus(status);
                batch.setErrorDetail(errorDetail);
                batchRepository.save(batch);
            });
        } catch (Exception e) {
            log.error("Failed to update batch status: {}", e.getMessage());
        }
    }
}

