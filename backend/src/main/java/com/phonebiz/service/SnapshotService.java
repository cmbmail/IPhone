package com.phonebiz.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Slf4j
public class SnapshotService {

    @Autowired
    private PhoneSnapshotRepository phoneSnapshotRepository;
    @Autowired
    private PhoneNumberRepository phoneNumberRepository;
    @Autowired
    private OrgStructureRepository orgStructureRepository;
    @Autowired
    private CostCenterMappingRepository costCenterMappingRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    // Self-injection for @Async proxy (avoids self-invocation bypass)
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private SnapshotService self;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int BATCH_SIZE = 500;

    // ==================== Scheduled + Trigger ====================

    @Scheduled(cron = "0 0 3 1 * ?")
    public void executeMonthlySnapshot() {
        YearMonth lastMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
        String snapshotMonth = lastMonth.format(MONTH_FORMATTER);
        
        log.info("Scheduled: generating monthly snapshot for {}", snapshotMonth);
        
        try {
            generateSnapshot(snapshotMonth);
            log.info("Monthly snapshot completed for {}", snapshotMonth);
        } catch (Exception e) {
            log.error("Monthly snapshot failed for {}", snapshotMonth, e);
            self.asyncRetrySnapshot(snapshotMonth, 1);
        }
    }

    @Async("importTaskExecutor")
    public void asyncRetrySnapshot(String snapshotMonth, int attempt) {
        if (attempt > 3) {
            log.error("Monthly snapshot retry limit reached for {}", snapshotMonth);
            return;
        }
        try {
            Thread.sleep(5000L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        log.info("Retrying snapshot for {}, attempt {}", snapshotMonth, attempt);
        try {
            generateSnapshot(snapshotMonth);
            log.info("Snapshot retry succeeded for {}", snapshotMonth);
        } catch (Exception e) {
            log.error("Snapshot retry failed for {}, attempt {}", snapshotMonth, attempt, e);
            self.asyncRetrySnapshot(snapshotMonth, attempt + 1);
        }
    }

    // ==================== Generate ====================

    @Transactional
    public void generateSnapshot(String snapshotMonth) {
        if (phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth) > 0) {
            log.warn("Snapshot already exists for {}, skipping", snapshotMonth);
            return;
        }

        Map<Long, OrgStructure> orgMap = buildOrgMap();
        Map<Long, String> costCenterMap = buildCostCenterMap();
        Map<String, Employee> employeeMap = buildEmployeeMap();

        long totalPhones = phoneNumberRepository.count();
        log.info("Generating snapshot for {}: {} phones", snapshotMonth, totalPhones);

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
                batch.add(buildSnapshot(phone, snapshotMonth, orgMap, costCenterMap, employeeMap));
            }

            if (!batch.isEmpty()) {
                phoneSnapshotRepository.saveAll(batch);
                savedCount += batch.size();
            }

            hasMore = phonePage.hasNext();
            page++;
        }

        log.info("Snapshot generated for {}: {} records", snapshotMonth, savedCount);
    }

    @Transactional
    public void triggerSnapshot(String snapshotMonth) {
        validateMonthFormat(snapshotMonth);
        generateSnapshot(snapshotMonth);
    }

    @Transactional
    public void regenerateSnapshot(String snapshotMonth) {
        validateMonthFormat(snapshotMonth);
        phoneSnapshotRepository.deleteBySnapshotMonth(snapshotMonth);
        generateSnapshot(snapshotMonth);
        log.info("Snapshot regenerated for {}", snapshotMonth);
    }

    // ==================== Query (paged) ====================

    @Transactional(readOnly = true)
    public Page<PhoneSnapshot> getSnapshotsPaged(String snapshotMonth, Pageable pageable) {
        return phoneSnapshotRepository.findBySnapshotMonth(snapshotMonth, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhoneSnapshot> getSnapshotsByStatusPaged(String snapshotMonth, Integer status, Pageable pageable) {
        return phoneSnapshotRepository.findBySnapshotMonthAndStatus(snapshotMonth, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhoneSnapshot> getSnapshotsByOrgPaged(String snapshotMonth, Long orgId, Pageable pageable) {
        return phoneSnapshotRepository.findBySnapshotMonthAndOrgId(snapshotMonth, orgId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhoneSnapshot> getSnapshotsByBranchPaged(String snapshotMonth, Long branchOrgId, Pageable pageable) {
        return phoneSnapshotRepository.findBySnapshotMonthAndBranchOrgId(snapshotMonth, branchOrgId, pageable);
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

    // ==================== Stats ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getSnapshotStats(String snapshotMonth) {
        int total = phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth);
        
        Map<Integer, Integer> statusMap = new HashMap<>();
        List<Object[]> statusGroups = phoneSnapshotRepository.countGroupByStatus(snapshotMonth);
        for (Object[] row : statusGroups) {
            Integer status = (Integer) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count.intValue());
        }

        Map<Integer, Integer> allocMap = new HashMap<>();
        List<Object[]> allocGroups = phoneSnapshotRepository.countGroupByAllocationStatus(snapshotMonth);
        for (Object[] row : allocGroups) {
            Integer allocStatus = (Integer) row[0];
            Long count = (Long) row[1];
            allocMap.put(allocStatus, count.intValue());
        }

        // Branch distribution
        Map<Long, Integer> branchMap = new HashMap<>();
        List<Object[]> branchGroups = phoneSnapshotRepository.countGroupByBranchOrgId(snapshotMonth);
        for (Object[] row : branchGroups) {
            Long branchId = (Long) row[0];
            Long count = (Long) row[1];
            branchMap.put(branchId, count.intValue());
        }

        return Map.of(
            "total", total,
            "byStatus", statusMap,
            "byAllocationStatus", allocMap,
            "byBranch", branchMap
        );
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

    // ==================== Bill Association ====================

    /**
     * Link snapshot to a bill month. After generating snapshots for snapshotMonth,
     * link them to billMonth so allocation uses this snapshot.
     */
    @Transactional
    public int linkToBillMonth(String snapshotMonth, String billMonth) {
        validateMonthFormat(snapshotMonth);
        validateMonthFormat(billMonth);
        
        List<PhoneSnapshot> snapshots = phoneSnapshotRepository.findBySnapshotMonth(snapshotMonth);
        int count = 0;
        for (PhoneSnapshot snapshot : snapshots) {
            snapshot.setBillMonth(billMonth);
            count++;
        }
        phoneSnapshotRepository.saveAll(snapshots);
        log.info("Linked {} snapshots from {} to bill month {}", count, snapshotMonth, billMonth);
        return count;
    }

    /**
     * Get snapshots linked to a specific bill month (used by allocation).
     */
    @Transactional(readOnly = true)
    public List<PhoneSnapshot> getSnapshotsByBillMonth(String billMonth) {
        return phoneSnapshotRepository.findByBillMonth(billMonth);
    }

    // ==================== Private helpers ====================

    private Map<Long, OrgStructure> buildOrgMap() {
        Map<Long, OrgStructure> map = new HashMap<>();
        List<OrgStructure> orgs = orgStructureRepository.findAll();
        for (OrgStructure org : orgs) {
            map.put(org.getId(), org);
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
                                       Map<Long, OrgStructure> orgMap,
                                       Map<Long, String> costCenterMap,
                                       Map<String, Employee> employeeMap) {
        Long orgId = phone.getOrgId() != null ? phone.getOrgId() : phone.getAllocationOrgId();
        String orgName = null;
        Long branchOrgId = null;
        String branchName = null;
        String costCenterCode = null;

        if (orgId != null) {
            OrgStructure org = orgMap.get(orgId);
            if (org != null) {
                orgName = org.getName();
                costCenterCode = costCenterMap.get(orgId);
                // Derive branch from org path
                branchOrgId = resolveBranchOrgId(org, orgMap);
                if (branchOrgId != null) {
                    OrgStructure branch = orgMap.get(branchOrgId);
                    branchName = branch != null ? branch.getName() : null;
                }
            }
        }

        String employeeNo = phone.getEmployeeNo();
        Employee employee = employeeNo != null ? employeeMap.get(employeeNo) : null;

        return PhoneSnapshot.builder()
                .snapshotMonth(snapshotMonth)
                .phoneId(phone.getId())
                .phoneNumber(phone.getPhoneNumber())
                .extensionNumber(phone.getExtensionNumber())
                .status(phone.getStatus())
                .orgId(orgId)
                .orgName(orgName)
                .branchOrgId(branchOrgId)
                .branchName(branchName)
                .costCenterCode(costCenterCode)
                .employeeNo(employeeNo)
                .employeeName(employee != null ? employee.getName() : null)
                .isSurrendered(phone.getStatus() == PhoneNumber.PS_CANCELLED)
                .isAllocatable(phone.getStatus() == PhoneNumber.PS_STOPPED)
                .build();
    }

    /**
     * Derive branch org ID from org path.
     * Path format: /rootId/level1Id/level2Id/...
     * For a dept under a branch, the branch is at path segment 2 (0-indexed).
     */
    private Long resolveBranchOrgId(OrgStructure org, Map<Long, OrgStructure> orgMap) {
        if (org.getPath() == null) return null;
        
        // If the org itself is a branch (type=2), it is its own branch
        if (org.getType() != null && org.getType() == 2) {
            return org.getId();
        }
        
        // Walk up the parent chain to find branch
        Long parentId = org.getParentId();
        while (parentId != null) {
            OrgStructure parent = orgMap.get(parentId);
            if (parent == null) break;
            if (parent.getType() != null && parent.getType() == 2) {
                return parent.getId();
            }
            parentId = parent.getParentId();
        }
        
        return null;
    }

    private void validateMonthFormat(String month) {
        try {
            YearMonth.parse(month, MONTH_FORMATTER);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.SYS_002, "Month format error, expected yyyy-MM");
        }
    }
}
