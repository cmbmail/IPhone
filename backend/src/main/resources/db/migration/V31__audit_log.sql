-- V31: Create audit_log table for operation tracking
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(50) NOT NULL COMMENT 'Module name',
    operation VARCHAR(100) NOT NULL COMMENT 'Operation description',
    operator VARCHAR(50) NOT NULL COMMENT 'Who performed the operation',
    target_type VARCHAR(50) DEFAULT NULL COMMENT 'Target entity type',
    target_id VARCHAR(100) DEFAULT NULL COMMENT 'Target entity ID',
    detail TEXT DEFAULT NULL COMMENT 'Additional detail JSON',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'Client IP',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS or FAILED',
    error_message TEXT DEFAULT NULL COMMENT 'Error message if failed',
    cost_time BIGINT DEFAULT 0 COMMENT 'Execution time in ms',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Log timestamp',
    INDEX idx_audit_module (module),
    INDEX idx_audit_operator (operator),
    INDEX idx_audit_target (target_type, target_id),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit log table';
