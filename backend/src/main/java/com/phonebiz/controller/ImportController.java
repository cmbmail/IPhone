package com.phonebiz.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.common.EnumHelper;
import com.phonebiz.dto.PhoneImportDataRequest;
import com.phonebiz.dto.PhoneImportRequest;
import com.phonebiz.entity.ImportBatch;
import com.phonebiz.service.ImportService;
import org.springframework.security.core.Authentication;
import com.phonebiz.annotation.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/import")
@PreAuthorize("hasAuthority('phone:import') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @AuditLog(module = "import", operation = "Import 操作")
    @PostMapping("/phone/start")
    public ApiResponse<Map<String, String>> startPhoneImport(@RequestBody PhoneImportRequest request, Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        ImportBatch batch = importService.createBatch(0, operator);
        
        Map<String, String> result = new HashMap<>();
        result.put("batchId", batch.getBatchId());
        result.put("message", "Import batch created, ready to upload data");
        
        return ApiResponse.success(result);
    }

    @AuditLog(module = "import", operation = "Import 操作")
    @PostMapping("/phone/{batchId}/upload")
    public ApiResponse<Void> uploadPhoneData(@PathVariable String batchId,
                                            @Valid @RequestBody List<PhoneImportDataRequest> data,
                                            @RequestParam(defaultValue = "ERROR") String conflictStrategy,
                                            Authentication authentication) {
        PhoneImportRequest.ConflictStrategy strategy = EnumHelper.parse(
                PhoneImportRequest.ConflictStrategy.class, conflictStrategy);
        // M-07: Convert validated DTO to Map for service compatibility
        List<Map<String, Object>> dataList = data.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("phoneNumber", d.getPhoneNumber());
            map.put("extensionNumber", d.getExtensionNumber());
            map.put("orgName", d.getOrgName());
            map.put("employeeNo", d.getEmployeeNo());
            map.put("status", d.getStatus());
            map.put("remark", d.getRemark());
            return map;
        }).collect(Collectors.toList());
        importService.processImportAsync(batchId, dataList, strategy, authentication != null ? authentication.getName() : "system");
        
        return ApiResponse.success("Import started", null);
    }

    @GetMapping("/phone/{batchId}/status")
    public ApiResponse<ImportBatch> getImportStatus(@PathVariable String batchId) {
        ImportBatch batch = importService.getBatchStatus(batchId);
        return ApiResponse.success(batch);
    }

    @GetMapping("/phone/{batchId}/progress")
    public ApiResponse<Map<String, Integer>> getImportProgress(@PathVariable String batchId) {
        Map<String, Integer> result = new HashMap<>();
        result.put("progress", importService.getProgress(batchId));
        return ApiResponse.success(result);
    }
}
