package com.phonebiz.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SYSTEM_ERROR(500, "System error"),
    PARAM_VALIDATION_FAILED(400, "Parameter validation failed"),

    AUTH_001(1001, "Username not found"),
    AUTH_002(1002, "Password incorrect"),
    AUTH_003(1003, "Account locked"),
    AUTH_004(1004, "Unauthorized"),
    AUTH_005(1005, "Token expired"),
    AUTH_006(1006, "Token invalid"),
    AUTH_007(1007, "First login must change password"),

    ORG_001(2001, "Organization not found"),
    ORG_002(2002, "Organization name already exists"),
    ORG_003(2003, "Cycle detected in organization structure"),
    ORG_004(2004, "Organization cannot be deleted as it has children"),
    ORG_005(2005, "Organization depth exceeds limit"),

    EMP_001(3001, "Employee already exists"),
    EMP_002(3002, "Employee not found"),
    EMP_003(3003, "Employee number format invalid"),
    EMP_004(3004, "Phone number format invalid"),
    EMP_005(3005, "Email format invalid"),

    PHONE_001(4001, "Phone number not found"),
    PHONE_002(4002, "Phone number already exists"),
    PHONE_003(4003, "Phone number already allocated"),
    PHONE_004(4004, "Phone number not allocated"),
    PHONE_005(4005, "Phone status invalid for operation"),
    PHONE_006(4006, "Cannot operate on surrendered phone"),

    PHONE_100(4100, "Phone cannot be allocated"),
    PHONE_101(4101, "Extension number duplicate"),
    PHONE_102(4102, "Extension pool exhausted"),

    PHONE_200(4200, "Status cannot be changed"),
    PHONE_201(4201, "Surrender operation not allowed"),

    PHONE_300(4300, "Change target phone unavailable"),
    PHONE_301(4301, "Two phones lock conflict"),
    PHONE_302(4302, "Change user target not found"),
    PHONE_303(4303, "Change organization target not found"),

    POOL_001(5001, "Extension pool range overlaps"),
    POOL_002(5002, "Extension pool exhausted"),

    DEVICE_001(6001, "Device MAC address invalid"),
    DEVICE_002(6002, "Device already allocated"),
    DEVICE_003(6003, "Device not found"),
    DEVICE_004(6004, "Device status invalid for operation"),
    DEVICE_005(6005, "Device cannot be retired"),

    IMPORT_001(7001, "File format invalid"),
    IMPORT_002(7002, "Import process failed"),
    IMPORT_003(7003, "Import timeout"),

    WO_001(8001, "Work order not found"),
    WO_002(8002, "Status transition invalid"),

    BILL_001(9001, "Bill month not found"),
    BILL_002(9002, "Allocation amount invalid"),

    INV_001(10001, "Invoice OCR failed"),
    INV_002(10002, "Invoice already distributed"),

    SYS_001(11001, "System internal error"),
    SYS_002(11002, "Parameter validation failed");

    private final int code;
    private final String message;
}
