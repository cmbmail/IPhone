-- V08: 区号组织映射表
-- Date: 2026-05-15
-- Module: M08 区号对照

CREATE TABLE area_code_org_mapping (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    area_code     VARCHAR(10) NOT NULL COMMENT '区号，如010、021',
    org_id        BIGINT UNSIGNED NOT NULL COMMENT '归属组织ID',
    priority      INT NOT NULL DEFAULT 1 COMMENT '优先级（数值越小越优先）',
    created_by    VARCHAR(50) NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50) NULL,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_area_code (area_code),
    INDEX idx_org (org_id),
    INDEX idx_priority (priority),
    CONSTRAINT fk_area_org FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区号组织映射表';
