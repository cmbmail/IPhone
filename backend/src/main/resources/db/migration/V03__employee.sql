-- V03: 员工表
-- Date: 2026-05-15
-- Module: M03 员工管理

CREATE TABLE employee (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_no         VARCHAR(20) NOT NULL COMMENT '工号（6位，字母或数字）',
    name                VARCHAR(50) NOT NULL COMMENT '姓名',
    org_id             BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID',
    position           VARCHAR(50) NULL COMMENT '职位',
    phone              VARCHAR(20) NULL COMMENT '联系电话',
    email              VARCHAR(100) NULL COMMENT '邮箱',
    status             ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '在职状态',
    entry_date         DATE NULL COMMENT '入职日期',
    leave_date         DATE NULL COMMENT '离职日期',
    is_virtual         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否虚拟员工（离职保留虚工号用）',
    created_by         VARCHAR(50) NOT NULL COMMENT '创建人',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50) NOT NULL COMMENT '更新人',
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_no (employee_no),
    INDEX idx_org (org_id),
    INDEX idx_status (status),
    INDEX idx_is_virtual (is_virtual),
    CONSTRAINT fk_employee_org FOREIGN KEY (org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- Seed数据：虚拟员工（总部部门管理员）
INSERT INTO employee (employee_no, name, org_id, position, status, is_virtual, created_by, updated_by)
VALUES
    ('VIR-01', '虚拟员工-总部IT', 1, 'IT管理员', 'active', 1, 'system', 'system'),
    ('VIR-02', '虚拟员工-总部HR', 1, 'HR管理员', 'active', 1, 'system', 'system'),
    ('VIR-03', '虚拟员工-总部财务', 1, '财务管理员', 'active', 1, 'system', 'system'),
    ('VIR-04', '虚拟员工-财务专员', 1, '财务专员', 'active', 1, 'system', 'system');
