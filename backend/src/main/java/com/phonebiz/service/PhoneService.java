package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSurrenderRecord;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneHistoryRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.repository.PhoneSurrenderRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneNumberRepository phoneRepository;
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
        return phoneRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhonesByStatus(PhoneNumber.PhoneStatus status, Pageable pageable) {
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
        phone.setUserId(request.getUserId());
        phone.setExtensionNumber(request.getExtensionNumber());
        phone.setExtensionType(request.getExtensionType());
        phone.setOrgId(request.getOrgId());
        phone.setRemark(request.getRemark());
        phone.setStatus(PhoneNumber.PhoneStatus.idle);
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

        if (phone.getStatus() != PhoneNumber.PhoneStatus.idle) {
            throw new BusinessException(ErrorCode.PHONE_003);
        }

        if (!employeeRepository.existsByEmployeeNo(request.getUserId())) {
            throw new BusinessException(ErrorCode.EMP_002);
        }

        if (!orgRepository.existsById(request.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_001);
        }

        String fromUser = phone.getUserId();
        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        String fromStatus = phone.getStatus().name();

        phone.setUserId(request.getUserId());
        phone.setOrgId(request.getOrgId());
        phone.setStatus(PhoneNumber.PhoneStatus.active);
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
            "active",
            fromUser,
            request.getUserId(),
            fromOrg,
            String.valueOf(request.getOrgId()),
            operator,
            request.getWorkOrderNo(),
            request.getRemark()
        );

        log.info("Phone {} allocated to user {} by {}", phone.getPhoneNumber(), request.getUserId(), operator);
        return saved;
    }

    @Transactional
    public PhoneNumber reclaimPhone(PhoneReclaimRequest request, String operator) {
        Long phoneId = request.getPhoneId();

        PhoneNumber phone = phoneRepository.findByIdWithLock(phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));

        if (phone.getStatus() != PhoneNumber.PhoneStatus.active) {
            throw new BusinessException(ErrorCode.PHONE_004);
        }

        String fromUser = phone.getUserId();
        String fromOrg = phone.getOrgId() != null ? String.valueOf(phone.getOrgId()) : null;
        String fromStatus = phone.getStatus().name();

        phone.setUserId(null);
        phone.setOrgId(null);
        phone.setExtensionNumber(null);
        phone.setExtensionType(null);
        phone.setStatus(PhoneNumber.PhoneStatus.idle);
        phone.setUpdatedBy(operator);

        PhoneNumber saved = phoneRepository.save(phone);

        recordHistory(
            phoneId,
            "reclaim",
            fromStatus,
            "idle",
            fromUser,
            null,
            fromOrg,
            null,
            operator,
            request.getWorkOrderNo(),
            request.getReason() != null ? "Reason: " + request.getReason() : request.getRemark()
        );

        log.info("Phone {} reclaimed from user {} by {}", phone.getPhoneNumber(), fromUser, operator);
        return saved;
    }

    @Transactional
    public void recordHistory(Long phoneId, String action, String fromStatus, String toStatus,
                              String fromUser, String toUser, String fromOrg, String toOrg,
                              String operator, String workOrderNo, String remark) {
        PhoneNumber phone = getPhoneById(phoneId);

        PhoneHistory history = new PhoneHistory();
        history.setPhoneId(phoneId);
        history.setPhoneNumber(phone.getPhoneNumber());
        history.setAction(action);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setFromUser(fromUser);
        history.setToUser(toUser);
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
}
