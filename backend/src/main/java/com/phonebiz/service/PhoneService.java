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
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneChangeRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
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
}

