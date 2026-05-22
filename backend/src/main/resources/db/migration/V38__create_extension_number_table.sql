CREATE TABLE extension_number (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    extension_number VARCHAR(20) NOT NULL,
    pool_id BIGINT UNSIGNED NOT NULL,
    status ENUM('ALLOCATED','IDLE','AVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    user_name VARCHAR(100) NULL,
    dept_name VARCHAR(100) NULL,
    dept_org_id BIGINT UNSIGNED NULL,
    phone_number VARCHAR(20) NULL,
    phone_id BIGINT UNSIGNED NULL,
    work_order_id BIGINT UNSIGNED NULL,
    created_by VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL DEFAULT 'system',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ext_number (extension_number),
    KEY idx_pool (pool_id),
    KEY idx_status (status),
    KEY idx_dept (dept_org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed from existing phone_number data
INSERT INTO extension_number (extension_number, pool_id, status, user_name, dept_name, dept_org_id, phone_number, phone_id)
SELECT pn.extension_number, 1,
    CASE WHEN pn.user_id IS NOT NULL THEN 'ALLOCATED' ELSE 'AVAILABLE' END,
    CASE WHEN pn.user_id IS NOT NULL THEN CONCAT('用户', pn.user_id) ELSE NULL END,
    o.name,
    pn.org_id,
    pn.phone_number,
    pn.id
FROM phone_number pn
LEFT JOIN org_structure o ON pn.org_id = o.id
WHERE pn.extension_number IS NOT NULL;
