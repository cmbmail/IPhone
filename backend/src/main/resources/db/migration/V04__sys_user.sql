-- V04: 系统用户表
-- Date: 2026-05-15
-- Module: M04 认证授权

CREATE TABLE sys_user (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password_hash           VARCHAR(200) NOT NULL COMMENT '密码哈希（bcrypt）',
    employee_no             VARCHAR(20) NOT NULL COMMENT '关联员工号',
    role                    ENUM('admin', 'ops', 'finance', 'boss') NOT NULL COMMENT '角色',
    scope_org_id            BIGINT UNSIGNED NULL COMMENT '管理范围组织ID（NULL表示全局）',
    status                  ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '账户状态',
    login_fail_count        INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until            DATETIME NULL COMMENT '账户锁定截止时间',
    password_changed_at     DATETIME NULL COMMENT '密码最后修改时间',
    last_login_at           DATETIME NULL COMMENT '最后登录时间',
    created_by              VARCHAR(50) NOT NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(50) NOT NULL,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_employee_no (employee_no),
    INDEX idx_role (role),
    INDEX idx_status (status),
    CONSTRAINT fk_user_employee FOREIGN KEY (employee_no) REFERENCES employee(employee_no),
    CONSTRAINT fk_user_org FOREIGN KEY (scope_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- Seed数据：测试用户
-- 密码均为 admin123! / ops123! / boss123! / finance123!
-- 首次登录强制要求修改密码
INSERT INTO sys_user (username, password_hash, employee_no, role, status, created_by, updated_by)
VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.cE3G3o6F3Q1P0V5GkW', 'VIR-01', 'admin', 'active', 'system', 'system'),
    ('ops', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.cE3G3o6F3Q1P0V5GkW', 'VIR-02', 'ops', 'active', 'system', 'system'),
    ('boss', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.cE3G3o6F3Q1P0V5GkW', 'VIR-03', 'boss', 'active', 'system', 'system'),
    ('finance', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.cE3G3o6F3Q1P0V5GkW', NULL, 'finance', 'active', 'system', 'system');
