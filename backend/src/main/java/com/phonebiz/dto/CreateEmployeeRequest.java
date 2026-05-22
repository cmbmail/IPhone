package com.phonebiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "Employee number is required")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Employee number must be 6 uppercase alphanumeric characters")
    private String employeeNo;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    private Long orgId;

    @Size(max = 50, message = "Position must not exceed 50 characters")
    private String position;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;

    private String entryDate;

    private Boolean isVirtual = false;
}
