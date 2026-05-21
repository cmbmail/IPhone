package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneImportRequest {

    // @NotBlank(message = "批次ID不能为空") - made optional for startImport compatibility
    private String batchId;

    @Builder.Default
    private String conflictStrategy = "ERROR";
    
    public enum ConflictStrategy {
        ERROR,
        SKIP,
        OVERWRITE
    }
    
    public ConflictStrategy getConflictStrategyEnum() {
        try {
            return ConflictStrategy.valueOf(conflictStrategy.toUpperCase());
        } catch (Exception e) {
            return ConflictStrategy.ERROR;
        }
    }
}

