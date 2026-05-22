package com.phonebiz.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOwnershipImportConfirmRequest {
    private List<PhoneOwnershipImportDTO> items;
}
