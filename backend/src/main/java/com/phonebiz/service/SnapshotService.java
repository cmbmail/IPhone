package com.phonebiz.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.repository.PhoneSnapshotRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final PhoneSnapshotRepository phoneSnapshotRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final OrgStructureRepository orgStructureRepository;
    private final CostCenterMappingRepository costCenterMappingRepository;
    private final EmployeeRepository employeeRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int BATCH_SIZE = 500;

    @Scheduled(cron = "0 0 3 1 * ?")
    public void executeMonthlySnapshot() {
        YearMonth lastMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
        String snapshotMonth = lastMonth.format(MONTH_FORMATTER);
        
        log.info("开始执行月度快照，月份: {}", snapshotMonth);
        
        try {
            generateSnapshot(snapshotMonth);
            log.info("月度快照执行完成，月份: {}", snapshotMonth);
        } catch (Exception e) {
            log.error("月度快照执行失败，月份: {}", snapshotMonth, e);
            // Use @Async retry instead of Thread.sleep
            asyncRetrySnapshot(snapshotMonth, 1);
        }
    }

    @Async("importTaskExecutor")
    public void asyncRetrySnapshot(String snapshotMonth, int attempt) {
        if (attempt > 3) {
            log.error("月度快照重试次数已达上限，月份: {}", snapshotMonth);
            return;
        }
        try {
            Thread.sleep(5000L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        log.info("月度快照重试第 {} 次，月份: {}", attempt, snapshotMonth);
        try {
            generateSnapshot(snapshotMonth);
            log.info("月度快照重试成功，月份: {}", snapshotMonth);
        } catch (Exception e) {
            log.error("月度快照重试失败，第 {} 次，月份: {}", attempt, snapshotMonth, e);
            asyncRetrySnapshot(snapshotMonth, attempt + 1);
        }
    }

    @Transactional
    public void generateSnapshot(String snapshotMonth) {
        if (phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth) > 0) {
            log.warn("月度快照已存在，跳过生成，月份: {}", snapshotMonth);
            return;
        }

        // Pre-load reference data maps (batch, not per-entity)
        Map<Long, String> orgNameMap = buildOrgNameMap();
        Map<Long, String> costCenterMap = buildCostCenterMap();
        Map<String, Employee> employeeMap = buildEmployeeMap();

        // Process in batches instead of loading all phone numbers at once
        long totalPhones = phoneNumberRepository.count();
        log.info("待快照号码总数: {}", totalPhones);

        int savedCount = 0;
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            org.springframework.data.domain.Page<PhoneNumber> phonePage = 
                phoneNumberRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(page, BATCH_SIZE));
            
            if (phonePage.isEmpty()) {
                hasMore = false;
                break;
            }

            List<PhoneSnapshot> batch = new ArrayList<>();
            for (PhoneNumber phone : phonePage.getContent()) {
                Integer status = phone.getStatus();
                if (status != PhoneNumber.PS_ACTIVE &&
                    status != PhoneNumber.PS_STOPPED &&
                    status != PhoneNumber.PS_CANCELLED) {
                    continue;
                }
                batch.add(buildSnapshot(phone, snapshotMonth, orgNameMap, costCenterMap, employeeMap));
            }

            if (!batch.isEmpty()) {
                phoneSnapshotRepository.saveAll(batch);
                savedCount += batch.size();
            }

            hasMore = phonePage.hasNext();
            page++;
        }

        log.info("月度快照生成完成，月份: {}，记录数: {}", snapshotMonth, savedCount);
    }

    private Map<Long, String> buildOrgNameMap() {
        Map<Long, String> map = new HashMap<>();
        List<OrgStructure> orgs = orgStructureRepository.findAll();
        for (OrgStructure org : orgs) {
            map.put(org.getId(), org.getName());
        }
        return map;
    }

    private Map<Long, String> buildCostCenterMap() {
        Map<Long, String> map = new HashMap<>();
        List<CostCenterMapping> mappings = costCenterMappingRepository.findByStatus(CostCenterMapping.CC_ACTIVE);
        for (CostCenterMapping mapping : mappings) {
            map.put(mapping.getOrgId(), mapping.getCostCenterCode());
        }
        return map;
    }

    private Map<String, Employee> buildEmployeeMap() {
        Map<String, Employee> map = new HashMap<>();
        List<Employee> employees = employeeRepository.findByStatus(Employee.EMP_ACTIVE);
        for (Employee emp : employees) {
            map.put(emp.getEmployeeNo(), emp);
        }
        return map;
    }

    private PhoneSnapshot buildSnapshot(PhoneNumber phone, String snapshotMonth,
                                       Map<Long, String> orgNameMap,
                                       Map<Long, String> costCenterMap,
                                       Map<String, Employee> employeeMap) {
        Long orgId = phone.getAllocationOrgId();
        String orgName = orgId != null ? orgNameMap.get(orgId) : null;
        String costCenterCode = orgId != null ? costCenterMap.get(orgId) : null;

        String employeeNo = phone.getUserId();
        Employee employee = employeeNo != null ? employeeMap.get(employeeNo) : null;

        return PhoneSnapshot.builder()
                .snapshotMonth(snapshotMonth)
                .phoneId(phone.getId())
                .phoneNumber(phone.getPhoneNumber())
                .extension(phone.getExtensionNumber())
                .status(phone.getStatus())
                .orgId(orgId)
                .orgName(orgName)
                .costCenterCode(costCenterCode)
                .employeeNo(employeeNo)
                .employeeName(employee != null ? employee.getName() : null)
                .isSurrendered(phone.getStatus() == PhoneNumber.PS_CANCELLED)
                .isAllocatable(phone.getStatus() == PhoneNumber.PS_STOPPED)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PhoneSnapshot> getSnapshotsByMonth(String snapshotMonth) {
        return phoneSnapshotRepository.findBySnapshotMonth(snapshotMonth);
    }

    @Transactional(readOnly = true)
    public List<PhoneSnapshot> getSnapshotsByMonthAndOrg(String snapshotMonth, Long orgId) {
        return phoneSnapshotRepository.findBySnapshotMonthAndOrgId(snapshotMonth, orgId);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableMonths() {
        return phoneSnapshotRepository.findDistinctSnapshotMonths();
    }

    @Transactional(readOnly = true)
    public PhoneSnapshot getSnapshot(Long id) {
        return phoneSnapshotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public int getSnapshotCount(String snapshotMonth) {
        return phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth);
    }

    @Transactional(readOnly = true)
    public int getSnapshotCountByStatus(String snapshotMonth, Integer status) {
        return phoneSnapshotRepository.countBySnapshotMonthAndStatus(snapshotMonth, status);
    }

    @Transactional(readOnly = true)
    public int getSnapshotCountByOrg(String snapshotMonth, Long orgId) {
        return phoneSnapshotRepository.countBySnapshotMonthAndOrgId(snapshotMonth, orgId);
    }

    @Transactional
    public void triggerSnapshot(String snapshotMonth) {
        validateMonthFormat(snapshotMonth);
        generateSnapshot(snapshotMonth);
    }

    private void validateMonthFormat(String month) {
        try {
            YearMonth.parse(month, MONTH_FORMATTER);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.SYS_002, "月份格式错误，应为 yyyy-MM");
        }
    }

    @Transactional
    public void regenerateSnapshot(String snapshotMonth) {
        validateMonthFormat(snapshotMonth);
        phoneSnapshotRepository.deleteBySnapshotMonth(snapshotMonth);
        generateSnapshot(snapshotMonth);
        log.info("月度快照已重新生成，月份: {}", snapshotMonth);
    }
}
