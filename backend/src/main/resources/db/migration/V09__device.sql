-- V09: 设备表
-- Date: 2026-05-15
-- Module: M09 设备管理

CREATE TABLE device (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(50) NOT NULL COMMENT '设备唯一标识',
    device_name         VARCHAR(100) NULL COMMENT '设备名称',
    device_type         ENUM('IP_PHONE', 'SOFT_PHONE', 'ATA', 'GATEWAY') NOT NULL COMMENT '设备类型',
    model               VARCHAR(100) NULL COMMENT '设备型号',
    mac_address         VARCHAR(20) NULL COMMENT 'MAC地址',
    ip_address          VARCHAR(50) NULL COMMENT 'IP地址',
    phone_number        VARCHAR(20) NULL COMMENT '绑定的电话号码',
    extension_number    VARCHAR(20) NULL COMMENT '绑定的分机号',
    status              ENUM('ONLINE', 'OFFLINE', 'UNREGISTERED', 'DISABLED') NOT NULL DEFAULT 'UNREGISTERED' COMMENT '设备状态',
    firmware_version    VARCHAR(50) NULL COMMENT '固件版本',
    last_checkin_time   DATETIME NULL COMMENT '最后心跳时间',
    remark              VARCHAR(500) NULL COMMENT '备注',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_id (device_id),
    UNIQUE KEY uk_mac_address (mac_address),
    INDEX idx_device_type (device_type),
    INDEX idx_status (status),
    INDEX idx_phone_number (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';
