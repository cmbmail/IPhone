package com.phonebiz.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            retrySnapshot(snapshotMonth, 1);
        }
    }

    private void retrySnapshot(String snapshotMonth, int attempt) {
        if (attempt > 3) {
            log.error("月度快照重试次数已达上限，月份: {}", snapshotMonth);
            return;
        }
        
        log.info("月度快照重试第 {} 次，月份: {}", attempt, snapshotMonth);
        
        try {
            Thread.sleep(5000L * attempt);
            generateSnapshot(snapshotMonth);
            log.info("月度快照重试成功，月份: {}", snapshotMonth);
        } catch (Exception e) {
            log.error("月度快照重试失败，第 {} 次，月份: {}", attempt, snapshotMonth, e);
            retrySnapshot(snapshotMonth, attempt + 1);
        }
    }

    @Transactional
    public void generateSnapshot(String snapshotMonth) {
        if (phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth) > 0) {
            log.warn("月度快照已存在，跳过生成，月份: {}", snapshotMonth);
            return;
        }

        List<PhoneNumber> phoneNumbers = phoneNumberRepository.findAll();
        log.info("查询到待快照号码数量: {}", phoneNumbers.size());

        Map<Long, String> orgNameMap = buildOrgNameMap();
        Map<Long, String> costCenterMap = buildCostCenterMap();
        Map<String, Employee> employeeMap = buildEmployeeMap();

        int savedCount = 0;
        for (PhoneNumber phone : phoneNumbers) {
            PhoneNumber.PhoneStatus status = phone.getStatus();
            if (status != PhoneNumber.PhoneStatus.active &&
                status != PhoneNumber.PhoneStatus.stopped &&
                status != PhoneNumber.PhoneStatus.cancelled) {
                continue;
            }

            PhoneSnapshot snapshot = buildSnapshot(phone, snapshotMonth, orgNameMap, costCenterMap, employeeMap);
            phoneSnapshotRepository.save(snapshot);
            savedCount++;
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
        List<CostCenterMapping> mappings = costCenterMappingRepository.findByStatus(CostCenterMapping.CostCenterStatus.active);
        for (CostCenterMapping mapping : mappings) {
            map.put(mapping.getOrgId(), mapping.getCostCenterCode());
        }
        return map;
    }

    private Map<String, Employee> buildEmployeeMap() {
        Map<String, Employee> map = new HashMap<>();
        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.active);
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
                .status(phone.getStatus().name())
                .orgId(orgId)
                .orgName(orgName)
                .costCenterCode(costCenterCode)
                .employeeNo(employeeNo)
                .employeeName(employee != null ? employee.getName() : null)
                .isSurrendered(phone.getStatus() == PhoneNumber.PhoneStatus.cancelled)
                .isAllocatable(phone.getStatus() == PhoneNumber.PhoneStatus.stopped)
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
    public int getSnapshotCountByStatus(String snapshotMonth, String status) {
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