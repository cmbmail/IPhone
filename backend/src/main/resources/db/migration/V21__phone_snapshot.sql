CREATE TABLE phone_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_month VARCHAR(7) NOT NULL,
    phone_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    extension VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    org_id BIGINT,
    org_name VARCHAR(200),
    cost_center_code VARCHAR(50),
    employee_no VARCHAR(50),
    employee_name VARCHAR(100),
    is_surrendered BOOLEAN DEFAULT FALSE,
    is_allocatable BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot_month_phone (snapshot_month, phone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度号码快照表';