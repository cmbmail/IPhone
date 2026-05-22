CREATE TABLE phone_ownership (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    phone_number VARCHAR(20) NOT NULL,
    branch_org_id BIGINT UNSIGNED NULL,
    branch_name VARCHAR(100) NULL,
    dept_org_id BIGINT UNSIGNED NULL,
    dept_name VARCHAR(100) NULL,
    remark VARCHAR(500) NULL,
    created_by VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL DEFAULT 'system',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_number (phone_number),
    KEY idx_branch (branch_org_id),
    KEY idx_dept (dept_org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
