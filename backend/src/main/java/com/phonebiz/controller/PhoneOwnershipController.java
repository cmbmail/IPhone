package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.PhoneOwnershipImportDTO;
import com.phonebiz.entity.PhoneOwnership;
import com.phonebiz.service.PhoneOwnershipService;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/phone-ownership")
@PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class PhoneOwnershipController {

    private final PhoneOwnershipService phoneOwnershipService;

    @GetMapping
    public ApiResponse<Page<PhoneOwnership>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long branchOrgId,
            @PageableDefault(size = 20, sort = "phoneNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(phoneOwnershipService.search(keyword, branchOrgId, pageable));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAuthority('phone:assign') or hasRole('ADMIN')")
    @AuditLog(module = "phoneOwnership", operation = "编辑号码归属", targetType = "PhoneOwnership", targetId = "#id")
    public ApiResponse<PhoneOwnership> update(@PathVariable Long id,
                                              @RequestParam(required = false) Long branchOrgId,
                                              @RequestParam(required = false) Long deptOrgId,
                                              @RequestParam(required = false) String remark) {
        return ApiResponse.success(phoneOwnershipService.update(id, branchOrgId, deptOrgId, remark));
    }

    @PostMapping("/import-compare")
    @PreAuthorize("hasAuthority('phone:import') or hasRole('ADMIN')")
    public ApiResponse<List<PhoneOwnershipImportDTO>> importCompare(@RequestParam("file") MultipartFile file) throws Exception {
        return ApiResponse.success(phoneOwnershipService.compareImport(file));
    }

    @PostMapping("/import-confirm")
    @PreAuthorize("hasAuthority('phone:import') or hasRole('ADMIN')")
    @AuditLog(module = "phoneOwnership", operation = "导入确认号码归属", targetType = "PhoneOwnership")
    public ApiResponse<Integer> importConfirm(@RequestBody List<PhoneOwnershipImportDTO> items) {
        int count = phoneOwnershipService.confirmImport(items);
        return ApiResponse.success(count);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> export() {
        List<PhoneOwnership> all = phoneOwnershipService.listAll();
        StringBuilder sb = new StringBuilder();
        sb.append("电话号码,分行,部门,备注\n");
        for (PhoneOwnership po : all) {
            sb.append(po.getPhoneNumber()).append(',')
              .append(po.getBranchName() != null ? po.getBranchName() : "").append(',')
              .append(po.getDeptName() != null ? po.getDeptName() : "").append(',')
              .append(po.getRemark() != null ? po.getRemark() : "").append('\n');
        }
        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Add BOM for Excel to recognize UTF-8
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=phone_ownership.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(result);
    }
    @GetMapping("/by-level")
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<com.phonebiz.dto.OwnershipLevelDTO.LevelSummaryResponse> getByLevel(
            @RequestParam int level,
            @RequestParam(required = false) Long parentOrgId) {
        return ApiResponse.success(phoneOwnershipService.getOwnershipByLevel(level, parentOrgId));
    }
}
