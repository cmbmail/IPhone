-- V10: 设备操作记录表
-- Date: 2026-05-15
-- Module: M10 设备操作

CREATE TABLE device_operation (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(50) NOT NULL COMMENT '设备ID',
    operation_type      VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_data      JSON NULL COMMENT '操作数据',
    status              VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '操作状态',
    result_message      VARCHAR(500) NULL COMMENT '结果消息',
    executed_at         DATETIME NULL COMMENT '执行时间',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备操作记录表';
