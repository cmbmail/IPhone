package com.phonebiz.service;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.PhoneHistoryRepository;
import com.phonebiz.repository.PhoneNumberRepository;

/**
 * Phone query service: read-only operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneQueryService {

    private final PhoneNumberRepository phoneRepository;
    private final PhoneHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhones(Pageable pageable) {
        return phoneRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneById(Long id) {
        return phoneRepository.findById(id)
                .orElseThrow(() -> new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.PHONE_001));
    }

    @Transactional(readOnly = true)
    public PhoneNumber getPhoneByNumber(String phoneNumber) {
        return phoneRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new com.phonebiz.common.BusinessException(com.phonebiz.common.ErrorCode.PHONE_001));
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

    @Transactional(readOnly = true)
    public List<PhoneNumber> getIdlePhones() {
        return phoneRepository.findIdlePhones();
    }

    @Transactional(readOnly = true)
    public long countActiveByOrg(Long orgId) {
        return phoneRepository.countActiveByOrgId(orgId);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getBranchPoolPhones(Long branchOrgId) {
        return phoneRepository.findBranchPoolPhones(branchOrgId);
    }

    @Transactional(readOnly = true)
    public List<PhoneNumber> getSystemPoolPhones() {
        return phoneRepository.findSystemPoolPhones();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBranchPoolStats(Long branchOrgId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("poolCount", phoneRepository.countBranchPoolPhones(branchOrgId));
        stats.put("totalCount", phoneRepository.countAllBranchPhones(branchOrgId));
        stats.put("systemPoolCount", phoneRepository.countSystemPoolPhones());
        return stats;
    }
}
