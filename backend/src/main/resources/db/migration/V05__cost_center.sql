-- V05: 成本中心对照表
-- Date: 2026-05-15
-- Module: M05 成本中心

CREATE TABLE cost_center_mapping (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    org_id                  BIGINT UNSIGNED NOT NULL COMMENT '部门ID（仅集团总部各部门）',
    cost_center_name        VARCHAR(100) NOT NULL COMMENT '成本中心名称',
    cost_center_code        VARCHAR(50) NOT NULL COMMENT '成本中心代码',
    status                  ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
    created_by              VARCHAR(50) NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(50) NOT NULL,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_org_cc (org_id, cost_center_code),
    INDEX idx_cost_center_code (cost_center_code),
    CONSTRAINT fk_cc_org FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本中心对照表';
