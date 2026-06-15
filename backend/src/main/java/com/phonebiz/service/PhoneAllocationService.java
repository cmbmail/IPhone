package com.phonebiz.service;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.BranchAllocateRequest;
import com.phonebiz.dto.DeptAllocateRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.security.DataScope;

/**
 * Phone allocation service: allocate, reclaim, branch/dept two-phase allocation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneAllocationService {

    private final PhoneNumberRepository phoneRepository;
    private final OrgStructureRepository orgRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScope dataScope;
    private final PhoneCommandService commandService;
    private final PhoneQueryService queryService;

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

        commandService.recordHistory(phoneId, "allocate", fromStatus, PhoneNumber.PS_ACTIVE,
                fromEmployeeNo, request.getEmployeeNo(),
                fromOrg, String.valueOf(request.getOrgId()),
                operator, request.getWorkOrderNo(), request.getRemark());

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

        commandService.recordHistory(phoneId, "reclaim", fromStatus, PhoneNumber.PS_IDLE,
                fromEmployeeNo, null,
                fromOrg, null,
                operator, request.getWorkOrderNo(),
                request.getReason() != null ? "Reason: " + request.getReason() : request.getRemark());

        log.info("Phone {} reclaimed from user {} by {}", phone.getPhoneNumber(), fromEmployeeNo, operator);
        return saved;
    }

    @Transactional
    public List<PhoneNumber> branchAllocate(BranchAllocateRequest request, String operator) {
        Long branchOrgId = request.getBranchOrgId();

        OrgStructure branch = orgRepository.findById(branchOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));
        if (!branch.isBranch()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "目标必须是分行");
        }

        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        List<PhoneNumber> updated = new ArrayList<>();
        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() != null) {
                log.warn("Phone {} already in branch pool {}, skipping", phone.getPhoneNumber(), phone.getBranchOrgId());
                continue;
            }

            String fromBranch = null;
            String toBranch = String.valueOf(branchOrgId);

            phone.setBranchOrgId(branchOrgId);
            phone.setUpdatedBy(operator);

            PhoneNumber saved = phoneRepository.save(phone);

            commandService.recordHistory(saved.getId(), "branch_allocate", saved.getStatus(), saved.getStatus(),
                    saved.getEmployeeNo(), saved.getEmployeeNo(),
                    fromBranch, toBranch,
                    operator, null,
                    request.getRemark() != null ? "分配到分行: " + request.getRemark() : "分配到分行");

            updated.add(saved);
        }

        log.info("Allocated {} phones to branch {} by {}", updated.size(), branchOrgId, operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were allocated — they may already be in a branch pool");
        }
        return updated;
    }

    @Transactional
    public List<PhoneNumber> deptAllocate(DeptAllocateRequest request, String operator) {
        Long deptOrgId = request.getDeptOrgId();

        OrgStructure dept = orgRepository.findById(deptOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        Long scopeOrgId = dataScope.getCurrentScopeOrgId();
        if (scopeOrgId != null) {
            OrgStructure scopeOrg = orgRepository.findById(scopeOrgId).orElse(null);
            String scopePath = scopeOrg != null ? scopeOrg.getPath() : "/" + scopeOrgId;
            if (!dept.getPath().startsWith(scopePath + "/") && !dept.getPath().equals(scopePath)) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot allocate to department outside your scope");
            }
        }

        List<PhoneNumber> updated = new ArrayList<>();
        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() == null) {
                log.warn("Phone {} not in any branch pool, skipping", phone.getPhoneNumber());
                continue;
            }
            if (phone.getOrgId() != null) {
                log.warn("Phone {} already allocated to dept {}, skipping", phone.getPhoneNumber(), phone.getOrgId());
                continue;
            }

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

            commandService.recordHistory(saved.getId(), "dept_allocate", PhoneNumber.PS_IDLE, saved.getStatus(),
                    saved.getEmployeeNo(), saved.getEmployeeNo(),
                    fromOrg, toOrg,
                    operator, null,
                    request.getRemark() != null ? "分配到部门: " + request.getRemark() : "分配到部门");

            updated.add(saved);
        }

        log.info("Allocated {} phones to dept {} by {}", updated.size(), deptOrgId, operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were allocated — they may not be in a branch pool or already allocated to a department");
        }
        return updated;
    }

    @Transactional
    public List<PhoneNumber> deptRevoke(BranchAllocateRequest request, String operator) {
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() != null && !isBranchAccessible(phone.getBranchOrgId())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot revoke phones outside your branch scope");
            }
        }

        List<PhoneNumber> updated = new ArrayList<>();
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

            commandService.recordHistory(saved.getId(), "dept_revoke", PhoneNumber.PS_ACTIVE, PhoneNumber.PS_IDLE,
                    phone.getEmployeeNo(), null,
                    fromOrg, String.valueOf(saved.getBranchOrgId()),
                    operator, null,
                    request.getRemark() != null ? "从部门回收到分行池: " + request.getRemark() : "从部门回收到分行池");

            updated.add(saved);
        }

        log.info("Revoked {} phones from dept back to branch pool by {}", updated.size(), operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were revoked — they may not be allocated to any department");
        }
        return updated;
    }

    @Transactional
    public List<PhoneNumber> branchRevoke(BranchAllocateRequest request, String operator) {
        List<PhoneNumber> phones = phoneRepository.findByIdsForBranchUpdate(request.getPhoneIds());

        if (phones.size() != request.getPhoneIds().size()) {
            throw new BusinessException(ErrorCode.PHONE_001, "Some phone IDs not found");
        }

        for (PhoneNumber phone : phones) {
            if (phone.getBranchOrgId() != null && !isBranchAccessible(phone.getBranchOrgId())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Cannot revoke phones outside your branch scope");
            }
        }

        for (PhoneNumber phone : phones) {
            if (phone.getOrgId() != null) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED,
                    "Phone " + phone.getPhoneNumber() + " is still allocated to a department. Revoke from dept first.");
            }
        }

        List<PhoneNumber> updated = new ArrayList<>();
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

            commandService.recordHistory(saved.getId(), "branch_revoke", saved.getStatus(), saved.getStatus(),
                    saved.getEmployeeNo(), saved.getEmployeeNo(),
                    fromBranch, null,
                    operator, null,
                    request.getRemark() != null ? "从分行回收到系统池: " + request.getRemark() : "从分行回收到系统池");

            updated.add(saved);
        }

        log.info("Revoked {} phones from branch back to system pool by {}", updated.size(), operator);
        if (updated.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "No phones were revoked — they may not be in any branch pool");
        }
        return updated;
    }

    private boolean isBranchAccessible(Long branchOrgId) {
        Long scopeOrgId = dataScope.getCurrentScopeOrgId();
        if (scopeOrgId == null) return true;

        if (scopeOrgId.equals(branchOrgId)) return true;

        OrgStructure scopeOrg = orgRepository.findById(scopeOrgId).orElse(null);
        OrgStructure branchOrg = orgRepository.findById(branchOrgId).orElse(null);
        if (scopeOrg != null && branchOrg != null) {
            return branchOrg.getPath().startsWith(scopeOrg.getPath() + "/");
        }
        return false;
    }
}
