-- V02: 组织架构表
-- Date: 2026-05-15
-- Module: M02 组织架构

CREATE TABLE org_structure (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '组织ID',
    parent_id           BIGINT UNSIGNED NULL COMMENT '上级组织ID，NULL表示根节点（集团）',
    name                VARCHAR(100) NOT NULL COMMENT '组织名称',
    type                ENUM('group', 'subsidiary', 'dept') NOT NULL DEFAULT 'dept' COMMENT '类型：集团/子公司/部门',
    level               INT NOT NULL DEFAULT 0 COMMENT '层级深度（集团=0）',
    path                VARCHAR(500) NOT NULL COMMENT '路径（/集团ID/子公司ID/部门ID/...），查询用',
    status              ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '状态',
    created_by          VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_path (path),
    CONSTRAINT fk_org_parent FOREIGN KEY (parent_id) REFERENCES org_structure(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构表';

-- Seed数据：集团根节点
INSERT INTO org_structure (id, parent_id, name, type, level, path, status, created_by, updated_by)
VALUES (1, NULL, '招商银行总行', 'group', 0, '/1', 'active', 'system', 'system');
