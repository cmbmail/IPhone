package com.phonebiz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateEmployeeRequest {

    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    private Long orgId;

    @Size(max = 50, message = "Position must not exceed 50 characters")
    private String position;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String status;

    private String leaveDate;
}
