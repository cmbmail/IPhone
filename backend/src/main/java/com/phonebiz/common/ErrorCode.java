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

    DEVICE_001(6001, "Device not found"),
    DEVICE_002(6002, "Device ID already exists"),
    DEVICE_003(6003, "MAC address already exists"),
    DEVICE_004(6004, "Device MAC address invalid"),
    DEVICE_005(6005, "Device already allocated"),
    DEVICE_006(6006, "Device status invalid for operation"),
    DEVICE_007(6007, "Device cannot be retired"),

    IMPORT_001(7001, "File format invalid"),
    IMPORT_002(7002, "Import process failed"),
    IMPORT_003(7003, "Import timeout"),

    WO_001(8001, "Work order not found"),
    WO_002(8002, "Status transition invalid"),
    WO_003(8003, "Work order item not found"),
    WO_004(8004, "Invalid work order item status"),

    BILL_001(9001, "Bill month not found"),
    BILL_002(9002, "Allocation amount invalid"),

    INV_001(10001, "Invoice OCR failed"),
    INV_002(10002, "Invoice already distributed"),

    SYS_001(11001, "System internal error"),
    SYS_002(11002, "Parameter validation failed"),
    SYS_003(11003, "Feature flag not found"),
    SYS_004(11004, "Feature flag already exists"),

    SNAPSHOT_NOT_FOUND(12001, "Snapshot not found"),

    USER_001(15001, "用户不存在"),
    USER_002(15002, "用户名已存在"),
    USER_003(15003, "不能禁用自己"),
    USER_004(15004, "不能删除自己"),
    USER_005(15005, "该用户已有登录记录,无法删除"),

    ROLE_001(14001, "角色不存在"),
    ROLE_002(14002, "角色编码已存在"),
    ROLE_003(14003, "角色名称已存在"),
    ROLE_004(14004, "系统内置角色不允许删除"),
    ROLE_005(14005, "该角色下仍有用户,不允许删除"),

    DEVICE_MAC_INVALID(13001, "MAC格式无效（非12位十六进制）"),
    DEVICE_MAC_DUPLICATE(13002, "MAC地址已存在"),
    DEVICE_NOT_STOCK(13003, "话机非库存状态,无法分配"),
    DEVICE_HAS_BOUND_PHONES(13004, "话机仍有绑定号码,请先解绑"),
    DEVICE_PHONE_ALREADY_BOUND(13005, "号码已绑定到该话机"),
    DEVICE_PHONE_NO_EXTENSION(13006, "目标号码无分机号,无法绑定"),
    DEVICE_STATUS_INVALID(13007, "话机状态无效"),
    DEVICE_NOT_ACTIVE(13008, "话机非在用状态"),
    DEVICE_NOT_REPAIRING(13009, "话机非维修中状态"),
    DEVICE_NOT_INACTIVE(13010, "话机非停用状态"),
    DEVICE_RETIRED(13011, "话机已报废,不可操作"),
    DEVICE_NOT_FOUND(13012, "话机不存在"),
    DEVICE_EMPLOYEE_NOT_FOUND(13013, "分配的员工不存在"),
    DEVICE_EMPLOYEE_INACTIVE(13014, "分配的员工非活跃状态"),
    DEVICE_ORG_NOT_FOUND(13015, "组织不存在"),
    DEVICE_PURCHASE_DATE_FUTURE(13016, "购置日期不能是未来日期");

    private final int code;
    private final String message;
}
