-- V06: 电话号码主表、历史表、拆机记录表
-- Date: 2026-05-15
-- Module: M06 号码基础

-- 电话号码主表
CREATE TABLE phone_number (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_number          VARCHAR(20) NOT NULL COMMENT '电话号码（全局唯一）',
    user_id               VARCHAR(20) NULL COMMENT '归属员工工号，为空表示未分配',
    extension_number      VARCHAR(10) NULL COMMENT '分机号（全局唯一）',
    extension_type        VARCHAR(20) NULL COMMENT '分机号类型：auto/manual',
    is_shared            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否共享号码',
    is_reentry           TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否二次入库',
    status               ENUM('idle', 'active', 'stopped', 'cancelled', 'reserved', 'disabled') NOT NULL DEFAULT 'idle' COMMENT '状态',
    org_id               BIGINT UNSIGNED NULL COMMENT '归属部门ID',
    allocation_org_id    BIGINT UNSIGNED NULL COMMENT '分机号分配来源组织',
    remark               VARCHAR(500) NULL COMMENT '备注',
    version              BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_by           VARCHAR(50) NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(50) NOT NULL,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone_number),
    INDEX idx_extension (extension_number),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_org (org_id),
    CONSTRAINT fk_phone_org FOREIGN KEY (org_id) REFERENCES org_structure(id),
    CONSTRAINT fk_phone_alloc_org FOREIGN KEY (allocation_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电话号码主表';

-- 号码操作历史表（永久留存）
CREATE TABLE phone_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id        BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id',
    phone_number    VARCHAR(20) NOT NULL COMMENT '快照：操作时的号码',
    action          VARCHAR(30) NOT NULL COMMENT '操作类型',
    from_status    VARCHAR(20) NULL COMMENT '变更前状态',
    to_status       VARCHAR(20) NULL COMMENT '变更后状态',
    from_user       VARCHAR(20) NULL COMMENT '变更前使用人',
    to_user         VARCHAR(20) NULL COMMENT '变更后使用人',
    from_org        VARCHAR(200) NULL COMMENT '变更前组织（path快照）',
    to_org          VARCHAR(200) NULL COMMENT '变更后组织（path快照）',
    work_order_no   VARCHAR(50) NULL COMMENT '关联工单号',
    operator        VARCHAR(50) NOT NULL COMMENT '操作人',
    operated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500) NULL,
    INDEX idx_phone (phone_id),
    INDEX idx_operated_at (operated_at DESC),
    INDEX idx_action (action),
    INDEX idx_work_order (work_order_no),
    CONSTRAINT fk_history_phone FOREIGN KEY (phone_id) REFERENCES phone_number(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号码操作历史（永久）';

-- 已拆机号码归档表（永久留存）
CREATE TABLE phone_surrender_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id        BIGINT UNSIGNED NOT NULL COMMENT '原phone_number.id',
    phone_number    VARCHAR(20) NOT NULL COMMENT '已拆机号码',
    final_user      VARCHAR(20) NULL COMMENT '最后使用人',
    final_org       VARCHAR(200) NULL COMMENT '最后所属组织',
    surrender_date  DATE NOT NULL COMMENT '拆机日期',
    surrender_type  VARCHAR(20) NOT NULL COMMENT '拆机类型（surrender/cancel）',
    operator        VARCHAR(50) NOT NULL COMMENT '操作人',
    work_order_no   VARCHAR(50) NULL COMMENT '关联工单号',
    remark          VARCHAR(500) NULL,
    archived_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_number (phone_number),
    INDEX idx_surrender_date (surrender_date),
    INDEX idx_surrender_type (surrender_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已拆机号码归档（永久）';
