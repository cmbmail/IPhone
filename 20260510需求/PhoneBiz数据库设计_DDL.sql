-- ============================================================
-- PhoneBiz 系统数据库设计 DDL
-- 数据库：MySQL 8
-- 版本：v1.1
-- 日期：2026-05-15
-- 修订内容：补充话机表、修正状态枚举、补充缺失列/表
-- ============================================================

-- ----------------------------------------------------------
-- 组织架构
-- ----------------------------------------------------------

CREATE TABLE org_structure (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '组织ID',
    parent_id     BIGINT UNSIGNED NULL COMMENT '上级组织ID，NULL表示根节点（集团）',
    name          VARCHAR(100) NOT NULL COMMENT '组织名称',
    type          ENUM('group', 'subsidiary', 'dept') NOT NULL DEFAULT 'dept' COMMENT '类型：集团/子公司/部门',
    level         INT NOT NULL DEFAULT 0 COMMENT '层级深度（集团=0）',
    path          VARCHAR(500) NOT NULL COMMENT '路径（/集团ID/子公司ID/部门ID/...），查询用',
    status        ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '状态',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL COMMENT '创建人',
    updated_by    VARCHAR(50) NOT NULL COMMENT '更新人',
    INDEX idx_parent (parent_id),
    INDEX idx_type (type),
    INDEX idx_path (path),
    FOREIGN KEY (parent_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构表';

-- ----------------------------------------------------------
-- 员工表
-- ----------------------------------------------------------

CREATE TABLE employee (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_no       VARCHAR(20) NOT NULL COMMENT '工号（6位，字母或数字）',
    name              VARCHAR(50) NOT NULL COMMENT '姓名',
    org_id            BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID',
    position          VARCHAR(50) NULL COMMENT '职位',
    phone             VARCHAR(20) NULL COMMENT '联系电话',
    email             VARCHAR(100) NULL,
    status            ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '在职状态',
    entry_date        DATE NULL COMMENT '入职日期',
    leave_date        DATE NULL COMMENT '离职日期',
    is_virtual        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否虚拟员工',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_by        VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_employee_no (employee_no),
    INDEX idx_org (org_id),
    INDEX idx_status (status),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- ----------------------------------------------------------
-- 成本中心对照表
-- ----------------------------------------------------------

CREATE TABLE cost_center_mapping (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    org_id              BIGINT UNSIGNED NOT NULL COMMENT '部门ID（仅集团总部各部门）',
    cost_center_name    VARCHAR(100) NOT NULL COMMENT '成本中心名称',
    cost_center_code    VARCHAR(50) NOT NULL COMMENT '成本中心代码',
    status              ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(50) NOT NULL,
    updated_by          VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_org_cc (org_id, cost_center_code),
    INDEX idx_cost_center_code (cost_center_code),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本中心对照表';

-- ----------------------------------------------------------
-- 电话号码主表
-- ----------------------------------------------------------

CREATE TABLE phone_number (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_number      VARCHAR(20) NOT NULL COMMENT '电话号码（全局唯一）',
    user_id           VARCHAR(20) NULL COMMENT '归属员工工号，为空表示未分配',
    extension_number  VARCHAR(10) NULL COMMENT '分机号（全局唯一）',
    extension_type    VARCHAR(20) NULL COMMENT '分机号类型：auto/manual',
    is_shared         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否共享号码',
    status            ENUM('idle', 'active', 'stopped', 'cancelled', 'reserved', 'disabled') NOT NULL DEFAULT 'idle' COMMENT '状态',
    org_id            BIGINT UNSIGNED NULL COMMENT '归属部门ID',
    allocation_org_id BIGINT UNSIGNED NULL COMMENT '分机号分配来源组织',
    is_reentry        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否二次入库',
    remark            VARCHAR(500) NULL COMMENT '备注',
    version           BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_by        VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_phone (phone_number),
    INDEX idx_extension (extension_number),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_org (org_id),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电话号码主表';

-- ----------------------------------------------------------
-- 分机号池（按部门分配）
-- ----------------------------------------------------------

CREATE TABLE extension_pool (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT UNSIGNED NOT NULL COMMENT '所属部门',
    start_number    VARCHAR(10) NOT NULL COMMENT '起始号码',
    end_number      VARCHAR(10) NOT NULL COMMENT '终止号码',
    allocated_by    VARCHAR(50) NOT NULL COMMENT '分配人（系统管理员）',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50) NULL,
    INDEX idx_org (org_id),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分机号池';

-- ----------------------------------------------------------
-- 区号组织映射表
-- ----------------------------------------------------------

CREATE TABLE area_code_org_mapping (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    area_code     VARCHAR(10) NOT NULL COMMENT '区号，如010、021',
    org_id        BIGINT UNSIGNED NOT NULL COMMENT '归属组织ID',
    priority      INT NOT NULL DEFAULT 1 COMMENT '优先级（数值越小越优先）',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_by    VARCHAR(50) NULL,
    INDEX idx_area_code (area_code),
    INDEX idx_org (org_id),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区号组织映射表';

-- ----------------------------------------------------------
-- 号码操作历史（永久留存）
-- ----------------------------------------------------------

CREATE TABLE phone_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id        BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id',
    phone_number    VARCHAR(20) NOT NULL COMMENT '快照：操作时的号码',
    action          VARCHAR(30) NOT NULL COMMENT '操作类型（allocate/reclaim/surrender/trouble/change_user/change_org/change_number/reserve/release/disable/enable）',
    from_status     VARCHAR(20) NULL COMMENT '变更前状态',
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
    FOREIGN KEY (phone_id) REFERENCES phone_number(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号码操作历史（永久）';

-- ----------------------------------------------------------
-- 已拆机号码归档（永久留存）
-- ----------------------------------------------------------

CREATE TABLE phone_surrender_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id        BIGINT UNSIGNED NOT NULL COMMENT '原phone_number.id',
    phone_number    VARCHAR(20) NOT NULL COMMENT '已拆机号码',
    final_user      VARCHAR(20) NULL COMMENT '最后使用人',
    final_org       VARCHAR(200) NULL COMMENT '最后所属组织',
    surrender_date  DATE NOT NULL COMMENT '拆机日期',
    surrender_type  VARCHAR(20) NOT NULL COMMENT '拆机类型（主动 surrender / 被动 cancel）',
    operator        VARCHAR(50) NOT NULL COMMENT '操作人',
    work_order_no   VARCHAR(50) NULL COMMENT '关联工单号',
    remark          VARCHAR(500) NULL,
    archived_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_number (phone_number),
    INDEX idx_surrender_date (surrender_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已拆机号码归档（永久）';

-- ----------------------------------------------------------
-- 电话机主表
-- ----------------------------------------------------------

CREATE TABLE phone_device (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    mac_address     VARCHAR(12) NOT NULL COMMENT 'MAC地址（大写12位十六进制，去冒号存储）',
    model           VARCHAR(100) NULL COMMENT '型号',
    brand           VARCHAR(100) NULL COMMENT '品牌',
    purchase_date   DATE NULL COMMENT '购置日期',
    org_id          BIGINT UNSIGNED NOT NULL COMMENT '归属组织',
    assigned_to     VARCHAR(20) NULL COMMENT '分配使用人工号（可为空，表示组织公共设备）',
    status          ENUM('stock','active','inactive','repairing','retired') NOT NULL DEFAULT 'stock',
    remark          VARCHAR(500) NULL COMMENT '备注',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_by      VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_mac (mac_address),
    INDEX idx_org (org_id),
    INDEX idx_assigned (assigned_to),
    INDEX idx_status (status),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电话机主表';

-- ----------------------------------------------------------
-- 话机-号码关联表（M:N）
-- ----------------------------------------------------------

CREATE TABLE device_phone_mapping (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT UNSIGNED NOT NULL COMMENT 'phone_device.id',
    phone_id    BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id（对应的分机号载体）',
    line_order  INT NOT NULL DEFAULT 1 COMMENT '线路序号',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_phone (device_id, phone_id),
    INDEX idx_phone (phone_id),
    FOREIGN KEY (device_id) REFERENCES phone_device(id),
    FOREIGN KEY (phone_id) REFERENCES phone_number(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话机-号码关联表（通过分机号绑定，phone_id对应的号码必须持有分机号）';

-- ----------------------------------------------------------
-- 话机操作历史
-- ----------------------------------------------------------

CREATE TABLE phone_device_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       BIGINT UNSIGNED NOT NULL,
    mac_address     VARCHAR(12) NOT NULL COMMENT '操作时MAC快照',
    action          VARCHAR(30) NOT NULL COMMENT '分配/回收/维修/报废/恢复/绑定/解绑',
    from_status     VARCHAR(20) NULL,
    to_status       VARCHAR(20) NULL,
    from_assigned   VARCHAR(20) NULL,
    to_assigned     VARCHAR(20) NULL,
    operator        VARCHAR(50) NOT NULL,
    operated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500) NULL,
    INDEX idx_device (device_id),
    INDEX idx_operated_at (operated_at DESC),
    FOREIGN KEY (device_id) REFERENCES phone_device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话机操作历史';

-- ----------------------------------------------------------
-- 号码导入批次
-- ----------------------------------------------------------

CREATE TABLE import_batch (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(200) NOT NULL COMMENT '上传文件名',
    total_count     INT NOT NULL DEFAULT 0 COMMENT '总条数',
    success_count   INT NOT NULL DEFAULT 0 COMMENT '成功条数',
    fail_count      INT NOT NULL DEFAULT 0 COMMENT '失败条数',
    error_detail    JSON NULL COMMENT '失败明细（JSON格式）',
    status          ENUM('pending', 'processing', 'done') NOT NULL DEFAULT 'pending',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号码导入批次表';

-- ----------------------------------------------------------
-- 系统功能开关
-- ----------------------------------------------------------

CREATE TABLE sys_feature_flag (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    feature_key   VARCHAR(100) NOT NULL COMMENT '功能标识：如WORK_ORDER_DRIVEN',
    feature_name  VARCHAR(100) NOT NULL COMMENT '功能名称',
    enabled       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启',
    scope_org_id  BIGINT UNSIGNED NULL COMMENT '范围组织ID（灰度发布用，NULL表示全局）',
    description   VARCHAR(500) NULL COMMENT '功能描述',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_by    VARCHAR(50) NULL,
    UNIQUE KEY uk_feature_scope (feature_key, scope_org_id),
    FOREIGN KEY (scope_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统功能开关';

-- ----------------------------------------------------------
-- 功能开关变更日志
-- ----------------------------------------------------------

CREATE TABLE sys_feature_flag_log (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    flag_id         BIGINT UNSIGNED NOT NULL COMMENT '关联sys_feature_flag.id',
    feature_key     VARCHAR(100) NOT NULL COMMENT '快照：功能标识',
    old_enabled     TINYINT(1) NULL COMMENT '变更前状态',
    new_enabled     TINYINT(1) NOT NULL COMMENT '变更后状态',
    operator        VARCHAR(50) NOT NULL COMMENT '操作人',
    operated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500) NULL,
    INDEX idx_flag (flag_id),
    INDEX idx_operated_at (operated_at DESC),
    FOREIGN KEY (flag_id) REFERENCES sys_feature_flag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能开关变更日志';

-- ----------------------------------------------------------
-- 工单主表
-- ----------------------------------------------------------

CREATE TABLE work_order (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    work_order_no       VARCHAR(50) NOT NULL COMMENT '工单编号（全局唯一）',
    type                VARCHAR(30) NOT NULL COMMENT '工单类型（allocate/change_user/change_number/change_org/reclaim/surrender/trouble/device_assign/device_reclaim/device_repair/device_retire/device_bind/device_unbind）',
    status              ENUM('pending', 'accepted', 'processing', 'completed', 'archived', 'cancelled') NOT NULL DEFAULT 'pending',
    priority            ENUM('low', 'normal', 'high', 'urgent') NOT NULL DEFAULT 'normal',
    description         VARCHAR(1000) NULL COMMENT '工单描述/原因',
    requester           VARCHAR(50) NOT NULL COMMENT '申请人（admin工号）',
    requester_org_id    BIGINT UNSIGNED NOT NULL COMMENT '申请人所属部门',
    handler             VARCHAR(50) NULL COMMENT '处理人（ops工号）',
    accepted_at         DATETIME NULL COMMENT '接收时间',
    completed_at        DATETIME NULL COMMENT '完成时间',
    archived_at         DATETIME NULL COMMENT '归档时间',
    batch_id            VARCHAR(50) NULL COMMENT '批次号（批量工单时同批次工单共享）',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(50) NOT NULL,
    updated_by          VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_work_order_no (work_order_no),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_batch (batch_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_requester (requester),
    FOREIGN KEY (requester_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单主表';

-- ----------------------------------------------------------
-- 工单项（批量工单：每条号码/话机一条记录）
-- ----------------------------------------------------------

CREATE TABLE work_order_item (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    work_order_id   BIGINT UNSIGNED NOT NULL COMMENT '关联工单ID',
    -- 号码快照
    phone_id        BIGINT UNSIGNED NULL COMMENT '关联号码ID',
    phone_number    VARCHAR(20) NULL COMMENT '电话号码',
    extension_number VARCHAR(10) NULL COMMENT '分机号快照',
    -- 话机快照
    device_id       BIGINT UNSIGNED NULL COMMENT '关联话机ID',
    mac_address     VARCHAR(12) NULL COMMENT 'MAC地址快照',
    -- 员工快照
    employee_no     VARCHAR(20) NULL COMMENT '相关员工工号',
    employee_name   VARCHAR(50) NULL COMMENT '员工姓名快照',
    -- 操作信息
    action          VARCHAR(30) NOT NULL COMMENT '操作类型（13种）',
    from_user       VARCHAR(20) NULL COMMENT '变更前使用人',
    to_user         VARCHAR(20) NULL COMMENT '变更后使用人',
    from_org        VARCHAR(200) NULL COMMENT '变更前组织',
    to_org          VARCHAR(200) NULL COMMENT '变更后组织',
    new_phone_number VARCHAR(20) NULL COMMENT '换号时新号码',
    new_org_id      BIGINT UNSIGNED NULL COMMENT '转移后部门ID',
    remark          VARCHAR(500) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_work_order (work_order_id),
    INDEX idx_phone (phone_id),
    INDEX idx_device (device_id),
    INDEX idx_employee (employee_no),
    FOREIGN KEY (work_order_id) REFERENCES work_order(id),
    FOREIGN KEY (phone_id) REFERENCES phone_number(id),
    FOREIGN KEY (device_id) REFERENCES phone_device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单项（含号码+话机+员工快照）';

-- ----------------------------------------------------------
-- 月度快照
-- ----------------------------------------------------------

CREATE TABLE phone_snapshot (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    snapshot_month          VARCHAR(7) NOT NULL COMMENT '快照月份（YYYY-MM）',
    phone_id                BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id',
    phone_number            VARCHAR(20) NOT NULL,
    user_id                 VARCHAR(20) NULL COMMENT '当月在用员工',
    extension_number        VARCHAR(10) NULL,
    status                  VARCHAR(20) NOT NULL COMMENT '快照时状态（active/stopped/cancelled/reserved/disabled/idle）',
    org_id                  BIGINT UNSIGNED NULL COMMENT '当月归属部门',
    org_name                VARCHAR(100) NULL COMMENT '部门名称快照',
    cost_center_code        VARCHAR(50) NULL COMMENT '成本中心代码快照',
    is_surrendered          TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已拆机（拆机号码纳入但标记）',
    is_allocatable          TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否参与分摊（idle号码=0）',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_month_phone (snapshot_month, phone_id),
    INDEX idx_month_org (snapshot_month, org_id),
    INDEX idx_month_status (snapshot_month, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度快照（账单分摊依据）';

-- ----------------------------------------------------------
-- 账单原始表（Excel导入）
-- ----------------------------------------------------------

CREATE TABLE bill_raw (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bill_month          VARCHAR(7) NOT NULL COMMENT '账单月份（YYYY-MM）',
    file_name           VARCHAR(200) NOT NULL COMMENT '原始文件名',
    phone_number        VARCHAR(20) NOT NULL COMMENT '电话号码',
    charge_amount       DECIMAL(12, 2) NOT NULL COMMENT '账单金额',
    charge_type         VARCHAR(50) NULL COMMENT '费用类型（通话费/月租/...）',
    billing_start_date  DATE NULL COMMENT '计费开始日期',
    billing_end_date    DATE NULL COMMENT '计费结束日期',
    raw_data            JSON NULL COMMENT '原始Excel行数据（完整JSON快照）',
    import_status       ENUM('pending', 'processed', 'error') NOT NULL DEFAULT 'pending',
    import_error_msg    VARCHAR(500) NULL,
    imported_by         VARCHAR(50) NOT NULL,
    imported_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_month (bill_month),
    INDEX idx_phone (phone_number),
    INDEX idx_status (import_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单原始表';

-- ----------------------------------------------------------
-- 账单分摊明细
-- ----------------------------------------------------------

CREATE TABLE bill_allocation (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bill_month            VARCHAR(7) NOT NULL COMMENT '账单月份',
    bill_raw_id           BIGINT UNSIGNED NOT NULL COMMENT '关联原始账单',
    phone_id              BIGINT UNSIGNED NULL COMMENT '关联号码（从snapshot匹配）',
    phone_number          VARCHAR(20) NOT NULL COMMENT '号码',
    snapshot_org_id       BIGINT UNSIGNED NULL COMMENT '快照中归属部门',
    snapshot_org_name     VARCHAR(100) NULL COMMENT '部门名称',
    cost_center_code      VARCHAR(50) NULL COMMENT '成本中心代码',
    charge_amount         DECIMAL(12, 2) NOT NULL COMMENT '分摊金额',
    anomaly_flag          TINYINT(1) NOT NULL DEFAULT 0 COMMENT '异常标记（0正常/1异常）',
    anomaly_reason        VARCHAR(500) NULL COMMENT '异常原因',
    -- 确认流程字段
    admin_confirm_org     ENUM('pending', 'correct', 'wrong') NOT NULL DEFAULT 'pending' COMMENT '行政确认部门',
    admin_confirm_amount   ENUM('pending', 'correct', 'wrong') NOT NULL DEFAULT 'pending' COMMENT '行政确认金额',
    finance_confirm_anomaly ENUM('pending', 'confirmed', 'rejected') NOT NULL DEFAULT 'pending' COMMENT '财务确认异常',
    finance_confirm_submit ENUM('pending', 'submitted') NOT NULL DEFAULT 'pending' COMMENT '财务最终提交',
    admin_confirm_by       VARCHAR(50) NULL COMMENT '行政确认人',
    admin_confirm_at       DATETIME NULL,
    finance_confirm_by     VARCHAR(50) NULL COMMENT '财务确认人',
    finance_confirm_at     DATETIME NULL,
    finance_submit_by      VARCHAR(50) NULL COMMENT '财务提交人',
    finance_submit_at       DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_month (bill_month),
    INDEX idx_org (snapshot_org_id),
    INDEX idx_anomaly (anomaly_flag),
    INDEX idx_confirm (finance_confirm_submit),
    FOREIGN KEY (bill_raw_id) REFERENCES bill_raw(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单分摊明细';

-- ----------------------------------------------------------
-- 子公司对账记录
-- ----------------------------------------------------------

CREATE TABLE subsidiary_reconciliation (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bill_month            VARCHAR(7) NOT NULL COMMENT '对账月份',
    subsidiary_org_id     BIGINT UNSIGNED NOT NULL COMMENT '子公司组织ID',
    total_amount          DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '分摊汇总金额',
    invoice_count         INT NOT NULL DEFAULT 0 COMMENT '发票数量',
    reconciliation_status ENUM('pending', 'confirmed_by_subsidiary', 'confirmed_by_group') NOT NULL DEFAULT 'pending',
    subsidiary_confirm_by  VARCHAR(50) NULL,
    subsidiary_confirm_at  DATETIME NULL,
    group_confirm_by       VARCHAR(50) NULL,
    group_confirm_at       DATETIME NULL,
    remark                VARCHAR(500) NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_month (bill_month),
    INDEX idx_subsidiary (subsidiary_org_id),
    FOREIGN KEY (subsidiary_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='子公司对账记录';

-- ----------------------------------------------------------
-- 发票主表
-- ----------------------------------------------------------

CREATE TABLE invoice (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_no            VARCHAR(100) NOT NULL COMMENT '发票号',
    bill_month            VARCHAR(7) NOT NULL COMMENT '发票对应账单月份',
    source_org_id         BIGINT UNSIGNED NOT NULL COMMENT '发票来源公司组织ID',
    source_org_name       VARCHAR(100) NOT NULL COMMENT '发票来源公司名称（从PDF识别）',
    recipient_org_id      BIGINT UNSIGNED NOT NULL COMMENT '接收方子公司组织ID',
    amount                DECIMAL(12, 2) NULL COMMENT '发票金额（可从PDF提取）',
    tax_amount            DECIMAL(12, 2) NULL COMMENT '税额',
    invoice_date          DATE NULL COMMENT '发票日期',
    status                ENUM('pending', 'distributed', 'read', 'confirmed') NOT NULL DEFAULT 'pending' COMMENT '状态',
    ocr_text              TEXT NULL COMMENT 'OCR识别的原始文本',
    ocr_confidence        DECIMAL(5, 4) NULL COMMENT 'OCR识别置信度',
    distribute_at         DATETIME NULL COMMENT '分发时间',
    read_at               DATETIME NULL COMMENT '接收方查看时间',
    confirmed_at          DATETIME NULL COMMENT '确认时间',
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invoice_no (invoice_no),
    INDEX idx_bill_month (bill_month),
    INDEX idx_recipient (recipient_org_id),
    INDEX idx_status (status),
    FOREIGN KEY (source_org_id) REFERENCES org_structure(id),
    FOREIGN KEY (recipient_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票主表';

-- ----------------------------------------------------------
-- 发票文件存储
-- ----------------------------------------------------------

CREATE TABLE invoice_file (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id    BIGINT UNSIGNED NOT NULL,
    file_name     VARCHAR(200) NOT NULL COMMENT '原始文件名（公司名称_序号.PDF）',
    file_path     VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size     BIGINT NOT NULL COMMENT '文件大小（字节）',
    md5           VARCHAR(32) NULL COMMENT '文件MD5校验',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票文件';

-- ----------------------------------------------------------
-- 发票分发记录
-- ----------------------------------------------------------

CREATE TABLE invoice_distribution (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT UNSIGNED NOT NULL,
    recipient_user  VARCHAR(50) NOT NULL COMMENT '接收人账号',
    distribution_status ENUM('success', 'failed') NOT NULL DEFAULT 'success',
    fail_reason     VARCHAR(200) NULL,
    notified_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoice (invoice_id),
    FOREIGN KEY (invoice_id) REFERENCES invoice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票分发记录';

-- ----------------------------------------------------------
-- 系统消息通知表（工单/发票通知）
-- ----------------------------------------------------------

CREATE TABLE sys_notification (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(30) NOT NULL COMMENT '通知类型（work_order/invoice/bill_allocation/phone/device/pool）',
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    target_user VARCHAR(50) NOT NULL COMMENT '接收人',
    target_role VARCHAR(30) NULL COMMENT '或按角色推送',
    related_id  BIGINT UNSIGNED NULL COMMENT '关联业务ID（工单ID/发票ID/号码ID/话机ID/...）',
    is_read     TINYINT(1) NOT NULL DEFAULT 0,
    read_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target_user (target_user),
    INDEX idx_type (type),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知';

-- ----------------------------------------------------------
-- 用户与角色表
-- ----------------------------------------------------------

CREATE TABLE sys_user (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(200) NOT NULL,
    employee_no         VARCHAR(20) NOT NULL COMMENT '关联员工号',
    role                VARCHAR(20) NOT NULL COMMENT 'admin/ops/finance/boss',
    scope_org_id        BIGINT UNSIGNED NULL COMMENT 'Admin管理范围（可NULL表示全局）',
    status              ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
    login_fail_count    INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    locked_until        DATETIME NULL COMMENT '账户锁定结束时间',
    password_changed_at DATETIME NULL COMMENT '密码最后修改时间（NULL表示从未修改，强制改密）',
    last_login_at       DATETIME NULL COMMENT '最后登录时间',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_employee_no (employee_no),
    INDEX idx_role (role),
    INDEX idx_status (status),
    FOREIGN KEY (employee_no) REFERENCES employee(employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------------------------------------
-- Flyway 版本历史表（由Flyway自动管理，无需手动创建）
-- ----------------------------------------------------------
-- 注意：以下表由Flyway自动创建，无需在此DDL中定义
-- CREATE TABLE flyway_schema_history (
--     installed_rank INT PRIMARY KEY,
--     version VARCHAR(50),
--     description VARCHAR(200),
--     type VARCHAR(20),
--     script VARCHAR(1000),
--     checksum INT,
--     installed_by VARCHAR(100),
--     installed_on DATETIME,
--     execution_time INT,
--     success TINYINT(1),
--     INDEX idx_succ (success)
-- );
