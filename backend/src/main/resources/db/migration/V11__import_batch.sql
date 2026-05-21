-- V11: 导入批次表
-- Date: 2026-05-15
-- Module: M11 数据导入

CREATE TABLE import_batch (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    batch_no            VARCHAR(50) NOT NULL COMMENT '批次号',
    import_type         VARCHAR(50) NOT NULL COMMENT '导入类型',
    file_name           VARCHAR(200) NOT NULL COMMENT '文件名',
    total_count         INT NOT NULL DEFAULT 0 COMMENT '总记录数',
    success_count       INT NOT NULL DEFAULT 0 COMMENT '成功数',
    fail_count          INT NOT NULL DEFAULT 0 COMMENT '失败数',
    status              VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT '批次状态',
    error_file_path     VARCHAR(500) NULL COMMENT '错误文件路径',
    remark              VARCHAR(500) NULL COMMENT '备注',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_no (batch_no),
    INDEX idx_import_type (import_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导入批次表';
