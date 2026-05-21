-- V19: 账单原始数据表
-- Date: 2026-05-15
-- Module: M19 账单管理

CREATE TABLE bill_raw (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bill_month          VARCHAR(7) NOT NULL COMMENT '账单月份',
    phone_number        VARCHAR(20) NOT NULL COMMENT '电话号码',
    total_amount        DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    call_amount         DECIMAL(10, 2) NULL COMMENT '通话费',
    data_amount         DECIMAL(10, 2) NULL COMMENT '流量费',
    sms_amount          DECIMAL(10, 2) NULL COMMENT '短信费',
    other_amount        DECIMAL(10, 2) NULL COMMENT '其他费用',
    status              VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bill_month (bill_month),
    INDEX idx_phone_number (phone_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单原始数据表';
