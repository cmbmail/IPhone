package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhoneImportDataRequest {

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number too long")
    private String phoneNumber;

    @Size(max = 20, message = "Extension number too long")
    private String extensionNumber;

    @Size(max = 100, message = "Org name too long")
    private String orgName;

    @Size(max = 50, message = "Employee number too long")
    private String employeeNo;

    @Size(max = 20, message = "Status too long")
    private String status;

    @Size(max = 500, message = "Remark too long")
    private String remark;
}
