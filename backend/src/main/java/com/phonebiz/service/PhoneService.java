package com.phonebiz.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.DeptAllocateRequest;
import com.phonebiz.dto.BranchAllocateRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneChangeRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneSurrenderRecord;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneHistoryRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.security.DataScope;
import com.phonebiz.repository.PhoneSurrenderRecordRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneNumberRepository phoneRepository;
    private final DataScope dataScope;
    private final PhoneHistoryRepository historyRepository;
    private final PhoneSurrenderRecordRepository surrenderRepository;
    private final EmployeeRepository employeeRepository;
    private final OrgStructureRepository orgRepository;

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhones(Pageable pageable) {
        return phoneRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneById(Long id) {
        return phoneRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneByNumber(String phoneNumber) {
        return phoneRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getPhonesByUser(String userId) {
        return phoneRepository.findByEmployeeNo(userId);
    }

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhonesByStatus(Integer status, Pageable pageable) {
        return phoneRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhoneHistory> getPhoneHistory(Long phoneId, Pageable pageable) {
        return historyRepository.findByPhoneIdOrderByOperatedAtDesc(phoneId, pageable);
    }

    @Transactional
    public PhoneNumber createPhone(CreatePhoneRequest request, String operator) {
        if (phoneRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException(ErrorCode.PHONE_002);
        }

        if (request.getExtensionNumber() != null && phoneRepository.existsByExtensionNumber(request.getExtensionNumber())) {
            throw new BusinessException(ErrorCode.PHONE_101);
        }

        PhoneNumber phone = new PhoneNumber();
        phone.setPhoneNumber(request.getPhoneNumber());
        phone.setEmployeeNo(request.getEmployeeNo());
        phone.setExtensionNumber(request.getExtensionNumber());
        phone.setExtensionType(request.getExtensionType());
        phone.setOrgId(request.getOrgId());
        phone.setRemark(request.getRemark());
        phone.setStatus(PhoneNumber.PS_IDLE);
        phone.setCreatedBy(operator);
        phone.setUpdatedBy(operator);

        return phoneRepository.save(phone);
    }

    @Transactional
    public PhoneNumber updatePhone(Long id, UpdatePhoneRequest request, String operator) {
        PhoneNumber phone = getPhoneById(id);

        if (request.getRemark() != null) {
            phone.setRemark(request.getRemark());
        }

        phone.setUpdatedBy(operator);
        return phoneRepository.save(phone);
    }

    @Transactional
    public PhoneNumber allocatePhone(PhoneAllocationRequest request, String operator) {
        Long phoneId = request.getPhoneId();

        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_IDLE) {
            throw new BusinessException(ErrorCode.PHONE_003);
        }

        if (!employeeRepository.existsByEmployeeNo(request.getEmployeeNo())) {
            throw new BusinessException(ErrorCode.EMP_002);
        }

        if (!orgRepository.existsById(request.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_001);
        }

        String fromEmployeeNo = phone.getEmployeeNo();
        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        Integer fromStatus = phone.getStatus();

        phone.setEmployeeNo(request.getEmployeeNo());
        phone.setOrgId(request.getOrgId());
        phone.setStatus(PhoneNumber.PS_ACTIVE);
        phone.setUpdatedBy(operator);

        if (request.getExtensionNumber() != null) {
            if (phoneRepository.existsByExtensionNumber(request.getExtensionNumber())) {
                throw new BusinessException(ErrorCode.PHONE_101);
            }
            phone.setExtensionNumber(request.getExtensionNumber());
            phone.setExtensionType("manual");
        }

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "allocate",
            fromStatus,
            PhoneNumber.PS_ACTIVE,
            fromEmployeeNo,
            request.getEmployeeNo(),
            fromOrg,
            String.valueOf(request.getOrgId()),
            operator,
            request.getWorkOrderNo(),
            request.getRemark()
        );

        log.info("Phone {} allocated to user {} by {}", phone.getPhoneNumber(), request.getEmployeeNo(), operator);
        return saved;
    }

    @Transactional
    public PhoneNumber reclaimPhone(PhoneReclaimRequest request, String operator) {
        Long phoneId = request.getPhoneId();

        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_ACTIVE) {
            throw new BusinessException(ErrorCode.PHONE_004);
        }

        String fromEmployeeNo = phone.getEmployeeNo();
        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        Integer fromStatus = phone.getStatus();

        phone.setEmployeeNo(null);
        phone.setOrgId(null);
        phone.setExtensionNumber(null);
        phone.setExtensionType(null);
        phone.setStatus(PhoneNumber.PS_IDLE);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "reclaim",
            fromStatus,
            PhoneNumber.PS_IDLE,
            fromEmployeeNo,
            null,
            fromOrg,
            null,
            operator,
            request.getWorkOrderNo(),
            request.getReason() != null ? "Reason: " + request.getReason() : request.getRemark()
        );

        log.info("Phone {} reclaimed from user {} by {}", phone.getPhoneNumber(), fromEmployeeNo, operator);
        return saved;
    }

    @Transactional
    public void recordHistory(Long phoneId, String action, Integer fromStatus, Integer toStatus,
                              String fromEmployeeNo, String toEmployeeNo, String fromOrg, String toOrg,
                              String operator, String workOrderNo, String remark) {
        PhoneNumber phone = getPhoneById(phoneId);

        PhoneHistory history = new PhoneHistory();
        history.setPhoneId(phoneId);
        history.setPhoneNumber(phone.getPhoneNumber());
        history.setAction(action);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setFromEmployeeNo(fromEmployeeNo);
        history.setToEmployeeNo(toEmployeeNo);
        history.setFromOrg(fromOrg);
        history.setToOrg(toOrg);
        history.setOperator(operator);
        history.setWorkOrderNo(workOrderNo);
        history.setRemark(remark);
        history.setOperatedAt(LocalDateTime.now());

        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getIdlePhones() {
        return phoneRepository.findIdlePhones();
    }

    @Transactional(readOnly = true)
    public long countActiveByOrg(Long orgId) {
        return phoneRepository.countActiveByOrgId(orgId);
    }

    @Transactional
    public PhoneNumber changeStatus(Long phoneId, Integer newStatus, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        Integer fromStatus = phone.getStatus();

        if (!isValidStatusTransition(phone.getStatus(), newStatus)) {
            throw new BusinessException(ErrorCode.PHONE_200);
        }

        phone.setStatus(newStatus);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "change_status",
            fromStatus,
            newStatus,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} status changed from {} to {} by {}", phone.getPhoneNumber(), fromStatus, newStatus, operator);
        return saved;
    }

    @Transactional
    public PhoneSurrenderRecord surrenderPhone(Long phoneId, Integer surrenderType, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (!canSurrender(phone.getStatus())) {
            throw new BusinessException(ErrorCode.PHONE_201);
        }

        PhoneSurrenderRecord record = new PhoneSurrenderRecord();
        record.setPhoneId(phoneId);
        record.setPhoneNumber(phone.getPhoneNumber());
        record.setFinalEmployeeNo(phone.getEmployeeNo());
        record.setFinalOrg(phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null);
        record.setSurrenderDate(LocalDate.now());
        record.setSurrenderType(surrenderType);
        record.setOperator(operator);
        record.setWorkOrderNo(workOrderNo);
        record.setRemark(remark);
        record.setArchivedAt(LocalDateTime.now());

        PhoneSurrenderRecord savedRecord = surrenderRepository.save(record);

        Integer fromStatus = phone.getStatus();
        phone.setStatus(PhoneNumber.PS_CANCELLED);
        phone.setEmployeeNo(null);
        phone.setOrgId(null);
        phone.setExtensionNumber(null);
        phone.setExtensionType(null);
        phone.setUpdatedBy(operator);
        phone.setIsReentry(true);
        phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "surrender",
            fromStatus,
            PhoneNumber.PS_CANCELLED,
            record.getFinalEmployeeNo(),
            null,
            record.getFinalOrg(),
            null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} surrendered with type {} by {}", phone.getPhoneNumber(), surrenderType, operator);
        return savedRecord;
    }

    @Transactional
    public PhoneNumber reservePhone(Long phoneId, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_IDLE) {
            throw new BusinessException(ErrorCode.PHONE_005);
        }

        Integer fromStatus = phone.getStatus();
        phone.setStatus(PhoneNumber.PS_RESERVED);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "reserve",
            fromStatus,
            PhoneNumber.PS_RESERVED,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} reserved by {}", phone.getPhoneNumber(), operator);
        return saved;
    }

    @Transactional
    public PhoneNumber releasePhone(Long phoneId, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_RESERVED) {
            throw new BusinessException(ErrorCode.PHONE_005);
        }

        Integer fromStatus = phone.getStatus();
        phone.setStatus(PhoneNumber.PS_IDLE);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "release",
            fromStatus,
            PhoneNumber.PS_IDLE,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} released by {}", phone.getPhoneNumber(), operator);
        return saved;
    }

    private boolean isValidStatusTransition(Integer from, Integer to) {
        return switch (from) {
            case 0 -> to == PhoneNumber.PS_ACTIVE || to == PhoneNumber.PS_RESERVED;
            case 1 -> to == PhoneNumber.PS_STOPPED || to == PhoneNumber.PS_IDLE;
            case 2 -> to == PhoneNumber.PS_ACTIVE || to == PhoneNumber.PS_CANCELLED;
            case 3 -> false;
            case 4 -> to == PhoneNumber.PS_IDLE || to == PhoneNumber.PS_ACTIVE;
            case 5 -> to == PhoneNumber.PS_IDLE;
            default -> false;
        };
    }

    private boolean canSurrender(Integer status) {
        return status == PhoneNumber.PS_ACTIVE || status == PhoneNumber.PS_STOPPED;
    }

    @Transactional
    public PhoneNumber changeUser(Long phoneId, String newEmployeeNo, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_ACTIVE) {
            throw new BusinessException(ErrorCode.PHONE_003);
        }

        if (!employeeRepository.existsByEmployeeNo(newEmployeeNo)) {
            throw new BusinessException(ErrorCode.EMP_002);
        }

        String fromEmployeeNo = phone.getEmployeeNo();
        Integer fromStatus = phone.getStatus();

        phone.setEmployeeNo(newEmployeeNo);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "change_user",
            fromStatus,
            fromStatus,
            fromEmployeeNo,
            newEmployeeNo,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} user changed from {} to {} by {}", phone.getPhoneNumber(), fromEmployeeNo, newEmployeeNo, operator);
        return saved;
    }

    @Transactional
    public PhoneNumber changeOrg(Long phoneId, Long newOrgId, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_ACTIVE) {
            throw new BusinessException(ErrorCode.PHONE_003);
        }

        if (!orgRepository.existsById(newOrgId)) {
            throw new BusinessException(ErrorCode.ORG_001);
        }

        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        Integer fromStatus = phone.getStatus();

        phone.setOrgId(newOrgId);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "change_org",
            fromStatus,
            fromStatus,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            fromOrg,
            String.valueOf(newOrgId),
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} org changed from {} to {} by {}", phone.getPhoneNumber(), fromOrg, newOrgId, operator);
        return saved;
    }

    @Transactional
    public PhoneNumber changeNumber(Long phoneId, String newPhoneNumber, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_ACTIVE && phone.getStatus() != PhoneNumber.PS_IDLE) {
            throw new BusinessException(ErrorCode.PHONE_003, "Cannot change number of phone in " + phone.getStatus() + " status");
        }

        if (phone.getPhoneNumber().equals(newPhoneNumber)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "New number is the same as current");
        }

        if (phoneRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new BusinessException(ErrorCode.PHONE_002);
        }

        String fromNumber = phone.getPhoneNumber();
        Integer fromStatus = phone.getStatus();

        phone.setPhoneNumber(newPhoneNumber);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "change_number",
            fromStatus,
            fromStatus,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark != null ? remark : "Number changed from " + fromNumber + " to " + newPhoneNumber
        );

        log.info("Phone {} number changed from {} to {} by {}", phoneId, fromNumber, newPhoneNumber, operator);
        return saved;
    }

    @Transactional
    public void swapPhoneNumbers(Long phoneId1, Long phoneId2, String operator, String workOrderNo, String remark) {
        if (phoneId1.equals(phoneId2)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot swap phone with itself");
        }

        List<PhoneNumber> phones = phoneRepository.findByIdsForUpdate(List.of(phoneId1, phoneId2));
        
        if (phones.size() != 2) {
            throw new BusinessException(ErrorCode.PHONE_001);
        }

        PhoneNumber phone1 = phones.get(0).getId().equals(phoneId1) ? phones.get(0) : phones.get(1);
        PhoneNumber phone2 = phones.get(0).getId().equals(phoneId2) ? phones.get(0) : phones.get(1);

        String phone1Number = phone1.getPhoneNumber();
        String phone2Number = phone2.getPhoneNumber();

        phone1.setPhoneNumber(phone2Number);
        phone2.setPhoneNumber(phone1Number);
        phone1.setUpdatedBy(operator);
        phone2.setUpdatedBy(operator);

        phoneRepository.saveAll(List.of(phone1, phone2));

        recordHistory(
            phone1.getId(),
            "swap_number",
            phone1.getStatus(),
            phone1.getStatus(),
            phone1.getEmployeeNo(),
            phone1.getEmployeeNo(),
            phone1.getOrgId() != null ? String.valueOf(phone1.getOrgId()) : null,
            phone1.getOrgId() != null ? String.valueOf(phone1.getOrgId()) : null,
            operator,
            workOrderNo,
            remark != null ? remark : "Swapped with phone " + phone2.getId()
        );

        recordHistory(
            phone2.getId(),
            "swap_number",
            phone2.getStatus(),
            phone2.getStatus(),
            phone2.getEmployeeNo(),
            phone2.getEmployeeNo(),
            phone2.getOrgId() != null ? String.valueOf(phone2.getOrgId()) : null,
            phone2.getOrgId() != null ? String.valueOf(phone2.getOrgId()) : null,
            operator,
            workOrderNo,
            remark != null ? remark : "Swapped with phone " + phone1.getId()
        );

        log.info("Phones {} and {} swapped numbers by {}", phoneId1, phoneId2, operator);
    }

    @Transactional
    public PhoneNumber changeExtension(Long phoneId, String newExtension, String operator, String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PS_ACTIVE && phone.getStatus() != PhoneNumber.PS_IDLE) {
            throw new BusinessException(ErrorCode.PHONE_003, "Cannot change extension of phone in " + phone.getStatus() + " status");
        }

        if (newExtension != null && phoneRepository.existsByExtensionNumber(newExtension)) {
            throw new BusinessException(ErrorCode.PHONE_101);
        }

        phone.setExtensionNumber(newExtension);
        phone.setExtensionType(newExtension != null ? "manual" : null);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "change_extension",
            phone.getStatus(),
            phone.getStatus(),
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} extension changed to {} by {}", phone.getPhoneNumber(), newExtension, operator);
        return saved;
    }

    @Transactional
    public PhoneNumber batchChange(Long phoneId, PhoneChangeRequest request, String operator) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        String fromEmployeeNo = phone.getEmployeeNo();
        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        String fromNumber = phone.getPhoneNumber();
        Integer fromStatus = phone.getStatus();
        boolean hasChanges = false;
        StringBuilder changeDetails = new StringBuilder();

        if (request.getEmployeeNo() != null && !request.getEmployeeNo().equals(phone.getEmployeeNo())) {
            if (!employeeRepository.existsByEmployeeNo(request.getEmployeeNo())) {
                throw new BusinessException(ErrorCode.EMP_002);
            }
            changeDetails.append("user: ").append(fromEmployeeNo).append("->").append(request.getEmployeeNo()).append("; ");
            phone.setEmployeeNo(request.getEmployeeNo());
            hasChanges = true;
        }

        if (request.getOrgId() != null && !request.getOrgId().equals(phone.getOrgId())) {
            if (!orgRepository.existsById(request.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_001);
            }
            changeDetails.append("org: ").append(fromOrg).append("->").append(request.getOrgId()).append("; ");
            phone.setOrgId(request.getOrgId());
            hasChanges = true;
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(phone.getPhoneNumber())) {
            if (phoneRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BusinessException(ErrorCode.PHONE_002);
            }
            changeDetails.append("number: ").append(fromNumber).append("->").append(request.getPhoneNumber()).append("; ");
            phone.setPhoneNumber(request.getPhoneNumber());
            hasChanges = true;
        }

        if (request.getExtensionNumber() != null) {
            if (phoneRepository.existsByExtensionNumber(request.getExtensionNumber())) {
                throw new BusinessException(ErrorCode.PHONE_101);
            }
            changeDetails.append("extension: ").append(phone.getExtensionNumber()).append("->").append(request.getExtensionNumber()).append("; ");
            phone.setExtensionNumber(request.getExtensionNumber());
            phone.setExtensionType("manual");
            hasChanges = true;
        }

        if (!hasChanges) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No changes provided");
        }

        phone.setUpdatedBy(operator);
        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "batch_change",
            fromStatus,
            fromStatus,
            fromEmployeeNo,
            phone.getEmployeeNo(),
            fromOrg,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            request.getWorkOrderNo(),
            request.getRemark() != null ? request.getRemark() : changeDetails.toString()
        );

        log.info("Phone {} batch changed: {}", phoneId, changeDetails);
        return saved;
    }

    @Transactional
    public int batchChangeMultiple(List<Long> phoneIds, PhoneChangeRequest request, String operator) {
        if (phoneIds == null || phoneIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Phone IDs cannot be empty");
        }

        int successCount = 0;
        for (Long phoneId : phoneIds) {
            try {
                batchChange(phoneId, request, operator);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to batch change phone {}: {}", phoneId, e.getMessage());
            }
        }

        log.info("Batch changed {} out of {} phones", successCount, phoneIds.size());
        return successCount;
    }

    @Transactional
    public PhoneNumber updateStatus(Long phoneId, Integer newStatus, String operator, 
                                    String workOrderNo, String remark) {
        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        Integer fromStatus = phone.getStatus();

        if (!isValidStatusTransition(phone.getStatus(), newStatus)) {
            throw new BusinessException(ErrorCode.PHONE_200);
        }

        phone.setStatus(newStatus);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "status_change",
            fromStatus,
            newStatus,
            phone.getEmployeeNo(),
            phone.getEmployeeNo(),
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null,
            operator,
            workOrderNo,
            remark
        );

        log.info("Phone {} status changed from {} to {} by {}", phone.getPhoneNumber(), fromStatus, newStatus, operator);
        return saved;
    }

    // ==================== Two-Phase Branch Allocation ====================

    /**
     * Phase 1: Allocate phones from system pool to a branch.
     * Sets branch_org_id on phone_number.
     */
    @Transactional
    public java.util.List<PhoneNumber> branchAllocate(BranchAllocateRequest request, String operator) {
        Long branchOrgId = request.getBranchOrgId();

        // Validate branch org exists and is a branch type (分行 type=2)
        OrgStructure branch = orgRepository.findById(branchOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));
        if (!branch.isBranch()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "目标必须是分行");
        }

        // Lock all phone records
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        List<PhoneNumber> updated = new java.util.ArrayList<>();
        for (PhoneNumber phone : phones) {
            // Must be in system pool (branch_org_id IS NULL)
            if (phone.getBranchOrgId() != null) {
                log.warn("Phone {} already in branch pool {}, skipping", phone.getPhoneNumber(), phone.getBranchOrgId());
                continue;
            }

            String fromBranch = null;
            String toBranch = String.valueOf(branchOrgId);

            phone.setBranchOrgId(branchOrgId);
            phone.setUpdatedBy(operator);

            PhoneNumber saved = phoneRepository.save(phone);

            recordHistory(
                saved.getId(),
                "branch_allocate",
                saved.getStatus(),
                saved.getStatus(),
                saved.getEmployeeNo(),
                saved.getEmployeeNo(),
                fromBranch,
                toBranch,
                operator,
                null,
                request.getRemark() != null ? "分配到分行: " + request.getRemark() : "分配到分行"
            );

            updated.add(saved);
        }

        log.info("Allocated {} phones to branch {} by {}", updated.size(), branchOrgId, operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were allocated — they may already be in a branch pool");
        }
        return updated;
    }

    /**
     * Phase 2: Allocate phones from branch pool to a specific department.
     * Sets org_id on phone_number (branch_org_id must already be set).
     * Validates that the dept belongs to the same branch.
     */
    @Transactional
    public java.util.List<PhoneNumber> deptAllocate(DeptAllocateRequest request, String operator) {
        Long deptOrgId = request.getDeptOrgId();

        // Validate dept org exists
        OrgStructure dept = orgRepository.findById(deptOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        // Lock all phone records
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        // Data scope check: if user has scopeOrgId (non-admin), validate access
        Long scopeOrgId = dataScope.getCurrentScopeOrgId();
        if (scopeOrgId != null) {
            // Branch admin can only allocate to depts under their branch
            OrgStructure scopeOrg = orgRepository.findById(scopeOrgId).orElse(null);
            String scopePath = scopeOrg != null ? scopeOrg.getPath() : "/" + scopeOrgId;
            if (!dept.getPath().startsWith(scopePath + "/") && !dept.getPath().equals(scopePath)) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot allocate to department outside your scope");
            }
        }

        List<PhoneNumber> updated = new java.util.ArrayList<>();
        for (PhoneNumber phone : phones) {
            // Must be in branch pool (branch_org_id set, org_id NULL)
            if (phone.getBranchOrgId() == null) {
                log.warn("Phone {} not in any branch pool, skipping", phone.getPhoneNumber());
                continue;
            }
            if (phone.getOrgId() != null) {
                log.warn("Phone {} already allocated to dept {}, skipping", phone.getPhoneNumber(), phone.getOrgId());
                continue;
            }

            // Validate dept belongs to the same branch
            Long phoneBranchId = phone.getBranchOrgId();
            if (!dept.getPath().contains("/" + phoneBranchId + "/") && !dept.getPath().equals("/" + phoneBranchId)) {
                OrgStructure branch = orgRepository.findById(phoneBranchId).orElse(null);
                if (branch == null || !dept.getPath().startsWith(branch.getPath() + "/")) {
                    throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                        "Phone " + phone.getPhoneNumber() + " belongs to a different branch than the target department");
                }
            }

            String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
            String toOrg = String.valueOf(deptOrgId);

            phone.setOrgId(deptOrgId);
            if (phone.getStatus() == PhoneNumber.PS_IDLE) {
                phone.setStatus(PhoneNumber.PS_ACTIVE);
            }
            phone.setUpdatedBy(operator);

            PhoneNumber saved = phoneRepository.save(phone);

            recordHistory(
                saved.getId(),
                "dept_allocate",
                PhoneNumber.PS_IDLE,
                saved.getStatus(),
                saved.getEmployeeNo(),
                saved.getEmployeeNo(),
                fromOrg,
                toOrg,
                operator,
                null,
                request.getRemark() != null ? "分配到部门: " + request.getRemark() : "分配到部门"
            );

            updated.add(saved);
        }

        log.info("Allocated {} phones to dept {} by {}", updated.size(), deptOrgId, operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were allocated — they may not be in a branch pool or already allocated to a department");
        }
        return updated;
    }

    /**
     * Return phone from department back to branch pool.
     * Clears org_id but keeps branch_org_id.
     */
    @Transactional
    public java.util.List<PhoneNumber> deptRevoke(BranchAllocateRequest request, String operator) {
        // Reuse BranchAllocateRequest: phoneIds + branchOrgId (for scope verification)
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        // Data scope check
        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() != null && !isBranchAccessible(phone.getBranchOrgId())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot revoke phones outside your branch scope");
            }
        }

        List<PhoneNumber> updated = new java.util.ArrayList<>();
        for (PhoneNumber phone : phones) {
            if (phone.getOrgId() == null) {
                log.warn("Phone {} not allocated to any dept, skipping", phone.getPhoneNumber());
                continue;
            }

            String fromOrg = String.valueOf(phone.getOrgId());

            phone.setOrgId(null);
            phone.setEmployeeNo(null);
            phone.setExtensionNumber(null);
            phone.setExtensionType(null);
            phone.setStatus(PhoneNumber.PS_IDLE);
            phone.setUpdatedBy(operator);

            PhoneNumber saved = phoneRepository.save(phone);

            recordHistory(
                saved.getId(),
                "dept_revoke",
                PhoneNumber.PS_ACTIVE,
                PhoneNumber.PS_IDLE,
                phone.getEmployeeNo(),
                null,
                fromOrg,
                String.valueOf(saved.getBranchOrgId()),
                operator,
                null,
                request.getRemark() != null ? "从部门回收到分行池: " + request.getRemark() : "从部门回收到分行池"
            );

            updated.add(saved);
        }

        log.info("Revoked {} phones from dept back to branch pool by {}", updated.size(), operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were revoked — they may not be allocated to any department");
        }
        return updated;
    }

    /**
     * Return phone from branch back to system pool.
     * Clears both org_id and branch_org_id.
     */
    @Transactional
    public java.util.List<PhoneNumber> branchRevoke(BranchAllocateRequest request, String operator) {
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        // Data scope check: admin can revoke any branch; others only their own
        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() != null && !isBranchAccessible(phone.getBranchOrgId())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot revoke phones outside your branch scope");
            }
        }

        // Validate no phone is allocated to a dept (org_id must be null)
        for (PhoneNumber phone : phones) {
            if (phone.getOrgId() != null) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                    "Phone " + phone.getPhoneNumber() + " is still allocated to a department. Revoke from dept first.");
            }
        }

        List<PhoneNumber> updated = new java.util.ArrayList<>();
        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() == null) {
                log.warn("Phone {} not in any branch pool, skipping", phone.getPhoneNumber());
                continue;
            }

            String fromBranch = String.valueOf(phone.getBranchOrgId());

            phone.setBranchOrgId(null);
            phone.setOrgId(null);
            phone.setUpdatedBy(operator);

            PhoneNumber saved = phoneRepository.save(phone);

            recordHistory(
                saved.getId(),
                "branch_revoke",
                saved.getStatus(),
                saved.getStatus(),
                saved.getEmployeeNo(),
                saved.getEmployeeNo(),
                fromBranch,
                null,
                operator,
                null,
                request.getRemark() != null ? "从分行回收到系统池: " + request.getRemark() : "从分行回收到系统池"
            );

            updated.add(saved);
        }

        log.info("Revoked {} phones from branch back to system pool by {}", updated.size(), operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were revoked — they may not be in any branch pool");
        }
        return updated;
    }

    /**
     * Check if current user's scope allows access to a given branch org.
     * Admin (scopeOrgId = root org, e.g., 1) can access all branches.
     * Branch admin can only access their own branch.
     */
    private boolean isBranchAccessible(Long branchOrgId) {
        Long scopeOrgId = dataScope.getCurrentScopeOrgId();
        if (scopeOrgId == null) return true; // no restriction

        if (scopeOrgId.equals(branchOrgId)) return true; // same branch

        // Check if scopeOrg is an ancestor of the branch org
        OrgStructure scopeOrg = orgRepository.findById(scopeOrgId).orElse(null);
        OrgStructure branchOrg = orgRepository.findById(branchOrgId).orElse(null);
        if (scopeOrg != null && branchOrg != null) {
            return branchOrg.getPath().startsWith(scopeOrg.getPath() + "/");
        }
        return false;
    }
    @Transactional(readOnly = true)
    public java.util.List<PhoneNumber> getBranchPoolPhones(Long branchOrgId) {
        return phoneRepository.findBranchPoolPhones(branchOrgId);
    }

    /**
     * Get system pool phones (branch_org_id IS NULL).
     */
    @Transactional(readOnly = true)
    public java.util.List<PhoneNumber> getSystemPoolPhones() {
        return phoneRepository.findSystemPoolPhones();
    }

    /**
     * Get branch pool statistics: count of pool phones + allocated phones per branch.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getBranchPoolStats(Long branchOrgId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("poolCount", phoneRepository.countBranchPoolPhones(branchOrgId));
        stats.put("totalCount", phoneRepository.countAllBranchPhones(branchOrgId));
        stats.put("systemPoolCount", phoneRepository.countSystemPoolPhones());
        return stats;
    }
}

