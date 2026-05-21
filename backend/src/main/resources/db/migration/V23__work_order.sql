-- ============================================================
-- PhoneBiz 工单系统表
-- ============================================================

-- ----------------------------------------------------------
-- 工单主表
-- ----------------------------------------------------------
CREATE TABLE work_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_no VARCHAR(50) NOT NULL COMMENT '工单编号（全局唯一）',
    type VARCHAR(30) NOT NULL COMMENT '工单类型',
    status ENUM('pending', 'accepted', 'processing', 'completed', 'archived', 'cancelled') NOT NULL DEFAULT 'pending',
    priority ENUM('low', 'normal', 'high', 'urgent') NOT NULL DEFAULT 'normal',
    title VARCHAR(200),
    description VARCHAR(1000) COMMENT '工单描述/原因',
    requester_id BIGINT,
    requester_name VARCHAR(100),
    requester_org_id BIGINT,
    handler_id BIGINT,
    handler_name VARCHAR(100),
    accepted_at DATETIME COMMENT '接收时间',
    completed_at DATETIME COMMENT '完成时间',
    archived_at DATETIME COMMENT '归档时间',
    batch_id VARCHAR(50) COMMENT '批次号（批量工单时同批次工单共享）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_work_order_no (work_order_no),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_batch (batch_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_requester (requester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单主表';

-- ----------------------------------------------------------
-- 工单项（批量工单：每条号码/话机一条记录）
-- ----------------------------------------------------------
CREATE TABLE work_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL COMMENT '关联工单ID',
    -- 通用字段
    item_type ENUM('phone', 'device', 'employee') NOT NULL DEFAULT 'phone',
    target_id BIGINT,
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    from_value VARCHAR(500),
    to_value VARCHAR(500),
    status ENUM('pending', 'processing', 'completed', 'failed', 'skipped') NOT NULL DEFAULT 'pending',
    executed_at DATETIME,
    error_message VARCHAR(500),
    operator VARCHAR(100),
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_work_order (work_order_id),
    INDEX idx_target (target_id),
    INDEX idx_status (status),
    FOREIGN KEY (work_order_id) REFERENCES work_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单项';

-- ----------------------------------------------------------
-- 系统功能开关
-- ----------------------------------------------------------
CREATE TABLE sys_feature_flag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feature_key VARCHAR(100) NOT NULL COMMENT '功能标识：如WORK_ORDER_DRIVEN',
    feature_name VARCHAR(100) NOT NULL COMMENT '功能名称',
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否开启',
    scope_type VARCHAR(20) COMMENT '范围类型：ALL/ORGANIZATION/USER',
    scope_value VARCHAR(500) COMMENT '范围值',
    description VARCHAR(500) COMMENT '功能描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_feature_key (feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统功能开关';

-- ----------------------------------------------------------
-- 初始化功能开关数据
-- ----------------------------------------------------------
INSERT INTO sys_feature_flag (feature_key, feature_name, is_enabled, scope_type, description, created_by, updated_by) VALUES
('phone.operation.work_order_driven', '工单驱动的号码操作', FALSE, 'ALL', '开启后号码操作需要通过工单流程', 'system', 'system'),
('phone.allocate.work_order_required', '号码分配需要工单', FALSE, 'ALL', '号码分配操作是否强制要求工单', 'system', 'system'),
('phone.reclaim.work_order_required', '号码回收需要工单', FALSE, 'ALL', '号码回收操作是否强制要求工单', 'system', 'system'),
('phone.surrender.work_order_required', '号码拆机需要工单', FALSE, 'ALL', '号码拆机操作是否强制要求工单', 'system', 'system');
