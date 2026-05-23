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
public class ExtensionDetailDTO {
    private String extensionNumber;
    private String phoneNumber;
    private String employeeName;
    private String branchName;
    private String deptName;
    private Long deptOrgId;
    private List<String> macAddresses;
}
