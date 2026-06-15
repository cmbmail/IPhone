package com.phonebiz.service;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.dto.BranchAllocateRequest;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.DeptAllocateRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneChangeRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSurrenderRecord;

/**
 * Phone service facade: delegates to PhoneQueryService, PhoneCommandService, PhoneAllocationService.
 * This class preserves the original public API for Controller compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneQueryService queryService;
    private final PhoneCommandService commandService;
    private final PhoneAllocationService allocationService;

    // ==================== Query delegates ====================

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhones(Pageable pageable) {
        return queryService.getPhones(pageable);
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneById(Long id) {
        return queryService.getPhoneById(id);
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneByNumber(String phoneNumber) {
        return queryService.getPhoneByNumber(phoneNumber);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getPhonesByUser(String userId) {
        return queryService.getPhonesByUser(userId);
    }

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhonesByStatus(Integer status, Pageable pageable) {
        return queryService.getPhonesByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PhoneHistory> getPhoneHistory(Long phoneId, Pageable pageable) {
        return queryService.getPhoneHistory(phoneId, pageable);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getIdlePhones() {
        return queryService.getIdlePhones();
    }

    @Transactional(readOnly = true)
    public long countActiveByOrg(Long orgId) {
        return queryService.countActiveByOrg(orgId);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getBranchPoolPhones(Long branchOrgId) {
        return queryService.getBranchPoolPhones(branchOrgId);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getSystemPoolPhones() {
        return queryService.getSystemPoolPhones();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBranchPoolStats(Long branchOrgId) {
        return queryService.getBranchPoolStats(branchOrgId);
    }

    // ==================== Command delegates ====================

    @Transactional
    public PhoneNumber createPhone(CreatePhoneRequest request, String operator) {
        return commandService.createPhone(request, operator);
    }

    @Transactional
    public PhoneNumber updatePhone(Long id, UpdatePhoneRequest request, String operator) {
        return commandService.updatePhone(id, request, operator);
    }

    @Transactional
    public void recordHistory(Long phoneId, String action, Integer fromStatus, Integer toStatus,
                              String fromEmployeeNo, String toEmployeeNo, String fromOrg, String toOrg,
                              String operator, String workOrderNo, String remark) {
        commandService.recordHistory(phoneId, action, fromStatus, toStatus,
                fromEmployeeNo, toEmployeeNo, fromOrg, toOrg,
                operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber changeStatus(Long phoneId, Integer newStatus, String operator, String workOrderNo, String remark) {
        return commandService.changeStatus(phoneId, newStatus, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneSurrenderRecord surrenderPhone(Long phoneId, Integer surrenderType, String operator, String workOrderNo, String remark) {
        return commandService.surrenderPhone(phoneId, surrenderType, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber reservePhone(Long phoneId, String operator, String workOrderNo, String remark) {
        return commandService.reservePhone(phoneId, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber releasePhone(Long phoneId, String operator, String workOrderNo, String remark) {
        return commandService.releasePhone(phoneId, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber changeUser(Long phoneId, String newEmployeeNo, String operator, String workOrderNo, String remark) {
        return commandService.changeUser(phoneId, newEmployeeNo, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber changeOrg(Long phoneId, Long newOrgId, String operator, String workOrderNo, String remark) {
        return commandService.changeOrg(phoneId, newOrgId, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber changeNumber(Long phoneId, String newPhoneNumber, String operator, String workOrderNo, String remark) {
        return commandService.changeNumber(phoneId, newPhoneNumber, operator, workOrderNo, remark);
    }

    @Transactional
    public void swapPhoneNumbers(Long phoneId1, Long phoneId2, String operator, String workOrderNo, String remark) {
        commandService.swapPhoneNumbers(phoneId1, phoneId2, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber changeExtension(Long phoneId, String newExtension, String operator, String workOrderNo, String remark) {
        return commandService.changeExtension(phoneId, newExtension, operator, workOrderNo, remark);
    }

    @Transactional
    public PhoneNumber batchChange(Long phoneId, PhoneChangeRequest request, String operator) {
        return commandService.batchChange(phoneId, request, operator);
    }

    @Transactional
    public int batchChangeMultiple(List<Long> phoneIds, PhoneChangeRequest request, String operator) {
        return commandService.batchChangeMultiple(phoneIds, request, operator);
    }

    @Transactional
    public PhoneNumber updateStatus(Long phoneId, Integer newStatus, String operator,
                                    String workOrderNo, String remark) {
        return commandService.updateStatus(phoneId, newStatus, operator, workOrderNo, remark);
    }

    // ==================== Allocation delegates ====================

    @Transactional
    public PhoneNumber allocatePhone(PhoneAllocationRequest request, String operator) {
        return allocationService.allocatePhone(request, operator);
    }

    @Transactional
    public PhoneNumber reclaimPhone(PhoneReclaimRequest request, String operator) {
        return allocationService.reclaimPhone(request, operator);
    }

    @Transactional
    public List<PhoneNumber> branchAllocate(BranchAllocateRequest request, String operator) {
        return allocationService.branchAllocate(request, operator);
    }

    @Transactional
    public List<PhoneNumber> deptAllocate(DeptAllocateRequest request, String operator) {
        return allocationService.deptAllocate(request, operator);
    }

    @Transactional
    public List<PhoneNumber> deptRevoke(BranchAllocateRequest request, String operator) {
        return allocationService.deptRevoke(request, operator);
    }

    @Transactional
    public List<PhoneNumber> branchRevoke(BranchAllocateRequest request, String operator) {
        return allocationService.branchRevoke(request, operator);
    }
}
