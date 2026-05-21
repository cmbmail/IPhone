-- V14: 通知表
-- Date: 2026-05-15
-- Module: M14 通知管理

CREATE TABLE notification (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(200) NOT NULL COMMENT '通知标题',
    content             TEXT NULL COMMENT '通知内容',
    type                VARCHAR(50) NOT NULL COMMENT '通知类型',
    priority            VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '优先级',
    status              VARCHAR(20) NOT NULL DEFAULT 'unread' COMMENT '状态',
    recipient           VARCHAR(50) NOT NULL COMMENT '接收人',
    read_at             DATETIME NULL COMMENT '阅读时间',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
