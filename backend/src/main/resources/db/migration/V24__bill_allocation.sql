-- V24: 账单分摊表
-- Date: 2026-05-15
-- Module: M24 费用管理

CREATE TABLE bill_allocation (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bill_month              VARCHAR(7) NOT NULL COMMENT '账单月份',
    bill_raw_id             BIGINT UNSIGNED NOT NULL COMMENT '原始账单ID',
    phone_id                BIGINT UNSIGNED NULL COMMENT '电话号码ID',
    phone_number            VARCHAR(20) NOT NULL COMMENT '电话号码',
    snapshot_org_id         BIGINT UNSIGNED NULL COMMENT '快照组织ID',
    snapshot_org_name       VARCHAR(100) NULL COMMENT '快照组织名称',
    cost_center_code        VARCHAR(50) NULL COMMENT '成本中心代码',
    charge_amount           DECIMAL(12,2) NOT NULL COMMENT '费用金额',
    anomaly_flag            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '异常标记',
    anomaly_reason          VARCHAR(500) NULL COMMENT '异常原因',
    admin_confirm_org       VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '集团确认组织状态',
    admin_confirm_amount    VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '集团确认金额状态',
    finance_confirm_anomaly VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '财务确认异常状态',
    finance_confirm_submit  VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '财务提交状态',
    admin_confirm_by        VARCHAR(50) NULL COMMENT '集团确认人',
    admin_confirm_at        DATETIME NULL COMMENT '集团确认时间',
    finance_confirm_by      VARCHAR(50) NULL COMMENT '财务确认人',
    finance_confirm_at      DATETIME NULL COMMENT '财务确认时间',
    finance_submit_by       VARCHAR(50) NULL COMMENT '财务提交人',
    finance_submit_at       DATETIME NULL COMMENT '财务提交时间',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bill_month (bill_month),
    INDEX idx_bill_raw_id (bill_raw_id),
    INDEX idx_phone_id (phone_id),
    INDEX idx_phone_number (phone_number),
    INDEX idx_snapshot_org_id (snapshot_org_id),
    INDEX idx_anomaly_flag (anomaly_flag),
    INDEX idx_admin_confirm_org (admin_confirm_org),
    INDEX idx_admin_confirm_amount (admin_confirm_amount),
    INDEX idx_finance_confirm_anomaly (finance_confirm_anomaly),
    INDEX idx_finance_confirm_submit (finance_confirm_submit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单分摊表';
