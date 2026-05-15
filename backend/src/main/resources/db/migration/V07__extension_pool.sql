-- V07: 分机号池表
-- Date: 2026-05-15
-- Module: M07 分机号池

CREATE TABLE extension_pool (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    org_id         BIGINT UNSIGNED NOT NULL COMMENT '所属部门',
    start_number   VARCHAR(10) NOT NULL COMMENT '起始号码',
    end_number     VARCHAR(10) NOT NULL COMMENT '终止号码',
    allocated_by   VARCHAR(50) NOT NULL COMMENT '分配人（系统管理员）',
    created_by     VARCHAR(50) NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(50) NULL,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org (org_id),
    CONSTRAINT fk_pool_org FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分机号池';
