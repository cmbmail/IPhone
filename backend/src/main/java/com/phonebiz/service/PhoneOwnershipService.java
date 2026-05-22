package com.phonebiz.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.dto.PhoneOwnershipImportDTO;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneOwnership;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneOwnershipRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneOwnershipService {

    private final PhoneOwnershipRepository phoneOwnershipRepository;
    private final OrgStructureRepository orgStructureRepository;

    public Page<PhoneOwnership> search(String keyword, Long branchOrgId, Pageable pageable) {
        return phoneOwnershipRepository.search(keyword, branchOrgId, pageable);
    }

    public List<PhoneOwnership> listAll() {
        return phoneOwnershipRepository.findAllByOrderByPhoneNumberAsc();
    }

    @Transactional
    public PhoneOwnership update(Long id, Long branchOrgId, Long deptOrgId, String remark) {
        PhoneOwnership po = phoneOwnershipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("号码归属记录不存在"));

        if (branchOrgId != null) {
            OrgStructure branch = orgStructureRepository.findById(branchOrgId).orElse(null);
            if (branch != null) {
                po.setBranchOrgId(branch.getId());
                po.setBranchName(branch.getBranchName());
            }
        } else {
            po.setBranchOrgId(null);
            po.setBranchName(null);
        }

        if (deptOrgId != null) {
            OrgStructure dept = orgStructureRepository.findById(deptOrgId).orElse(null);
            if (dept != null) {
                po.setDeptOrgId(dept.getId());
                po.setDeptName(dept.getName());
            }
        } else {
            po.setDeptOrgId(null);
            po.setDeptName(null);
        }

        po.setRemark(remark);
        return phoneOwnershipRepository.save(po);
    }

    public List<PhoneOwnershipImportDTO> compareImport(MultipartFile file) throws Exception {
        List<PhoneOwnershipImportDTO> result = new ArrayList<>();
        Map<String, PhoneOwnership> existingMap = buildExistingMap();
        Map<String, OrgStructure> orgByName = buildOrgByNameMap();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] cols = line.split(",", -1);
                if (cols.length < 1 || cols[0].trim().isEmpty()) continue;

                String phone = cols[0].trim();
                String branchName = cols.length > 1 ? cols[1].trim() : "";
                String deptName = cols.length > 2 ? cols[2].trim() : "";
                String remark = cols.length > 3 ? cols[3].trim() : "";

                PhoneOwnership existing = existingMap.get(phone);

                PhoneOwnershipImportDTO dto = PhoneOwnershipImportDTO.builder()
                        .phoneNumber(phone)
                        .branchName(branchName)
                        .deptName(deptName)
                        .remark(remark)
                        .isNew(existing == null)
                        .hasDiff(false)
                        .build();

                if (existing != null) {
                    dto.setExistingBranchName(existing.getBranchName());
                    dto.setExistingDeptName(existing.getDeptName());
                    dto.setExistingRemark(existing.getRemark());

                    boolean diff = !Objects.equals(nullIfEmpty(branchName), nullIfEmpty(existing.getBranchName()))
                            || !Objects.equals(nullIfEmpty(deptName), nullIfEmpty(existing.getDeptName()))
                            || !Objects.equals(nullIfEmpty(remark), nullIfEmpty(existing.getRemark()));
                    dto.setHasDiff(diff);
                }
                result.add(dto);
            }
        }
        return result;
    }

    @Transactional
    public int confirmImport(List<PhoneOwnershipImportDTO> items) {
        Map<String, OrgStructure> orgByName = buildOrgByNameMap();
        int count = 0;
        for (PhoneOwnershipImportDTO dto : items) {
            Optional<PhoneOwnership> existingOpt = phoneOwnershipRepository.findByPhoneNumber(dto.getPhoneNumber());
            PhoneOwnership po;
            if (existingOpt.isPresent()) {
                po = existingOpt.get();
            } else {
                po = PhoneOwnership.builder().phoneNumber(dto.getPhoneNumber()).build();
            }

            // Resolve branch
            String branchName = dto.getBranchName();
            if (branchName != null && !branchName.isEmpty()) {
                OrgStructure branch = orgByName.get(branchName);
                if (branch != null) {
                    po.setBranchOrgId(branch.getId());
                    po.setBranchName(branch.getBranchName());
                } else {
                    po.setBranchOrgId(null);
                    po.setBranchName(branchName);
                }
            }

            // Resolve dept
            String deptName = dto.getDeptName();
            if (deptName != null && !deptName.isEmpty()) {
                OrgStructure dept = orgByName.get(deptName);
                if (dept != null) {
                    po.setDeptOrgId(dept.getId());
                    po.setDeptName(dept.getName());
                } else {
                    po.setDeptOrgId(null);
                    po.setDeptName(deptName);
                }
            }

            po.setRemark(dto.getRemark());
            phoneOwnershipRepository.save(po);
            count++;
        }
        return count;
    }

    private Map<String, PhoneOwnership> buildExistingMap() {
        Map<String, PhoneOwnership> map = new HashMap<>();
        phoneOwnershipRepository.findAll().forEach(p -> map.put(p.getPhoneNumber(), p));
        return map;
    }

    private Map<String, OrgStructure> buildOrgByNameMap() {
        Map<String, OrgStructure> map = new HashMap<>();
        orgStructureRepository.findAll().forEach(o -> {
            map.put(o.getName(), o);
            if (o.getBranchName() != null && !o.getBranchName().equals(o.getName())) {
                map.putIfAbsent(o.getBranchName(), o);
            }
        });
        return map;
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
