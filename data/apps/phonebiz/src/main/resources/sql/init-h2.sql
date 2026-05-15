-- 组织架构表
CREATE TABLE IF NOT EXISTS sys_org (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    org_name VARCHAR(100) NOT NULL,
    org_code VARCHAR(50) NOT NULL,
    org_level INT DEFAULT 1,
    sort INT DEFAULT 0,
    contact VARCHAR(50),
    phone VARCHAR(20),
    address VARCHAR(200),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 员工表
CREATE TABLE IF NOT EXISTS sys_employee (
    id BIGINT PRIMARY KEY,
    org_id BIGINT,
    emp_no VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    gender INT DEFAULT 0,
    id_card VARCHAR(18),
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    menu_name VARCHAR(100) NOT NULL,
    menu_code VARCHAR(50) NOT NULL,
    menu_type VARCHAR(20),
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 员工角色关联表
CREATE TABLE IF NOT EXISTS sys_employee_role (
    employee_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (employee_id, role_id)
);

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- 电话号码表
CREATE TABLE IF NOT EXISTS phone_number (
    id BIGINT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'UNASSIGNED',
    employee_id BIGINT,
    employee_name VARCHAR(50),
    remark VARCHAR(500),
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 工单表
CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    order_type VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PENDING',
    phone_id BIGINT,
    phone_number VARCHAR(20),
    requester_id BIGINT,
    requester_name VARCHAR(50),
    handler_id BIGINT,
    handler_name VARCHAR(50),
    title VARCHAR(100),
    content CLOB,
    reason VARCHAR(500),
    result VARCHAR(500),
    priority INT DEFAULT 1,
    create_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(50),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag INT DEFAULT 0
);

-- 初始组织
INSERT INTO sys_org (id, parent_id, org_name, org_code, org_level, sort, status) VALUES
(1, 0, '招商银行总行', 'CMB_HQ', 1, 1, 1),
(2, 1, '信息技术部', 'CMB_IT', 2, 1, 1),
(3, 1, '财务部', 'CMB_FINANCE', 2, 2, 1);

-- 初始角色
INSERT INTO sys_role (id, role_name, role_code, sort, status) VALUES
(1, '超级管理员', 'ADMIN', 1, 1),
(2, '普通用户', 'USER', 2, 1);

-- 初始管理员用户（密码：admin123 的MD5值）
INSERT INTO sys_employee (id, org_id, emp_no, username, password, real_name, phone, status) VALUES
(1, 1, 'EMP001', 'admin', '0192023a7bbd73250516f069df18b500', '系统管理员', '13800138000', 1);

-- 初始菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, icon, sort, status) VALUES
(1, 0, '系统管理', 'SYSTEM', 'directory', '/system', 'setting', 1, 1),
(2, 1, '组织架构', 'ORG', 'menu', '/system/org', 'org', 1, 1),
(3, 1, '员工管理', 'EMPLOYEE', 'menu', '/system/employee', 'user', 2, 1),
(4, 1, '角色管理', 'ROLE', 'menu', '/system/role', 'role', 3, 1),
(5, 1, '菜单管理', 'MENU', 'menu', '/system/menu', 'menu', 4, 1),
(6, 0, '号码管理', 'PHONE', 'directory', '/phone', 'phone', 2, 1),
(7, 6, '号码列表', 'PHONE_LIST', 'menu', '/phone/list', 'list', 1, 1),
(8, 0, '工单管理', 'ORDER', 'directory', '/order', 'order', 3, 1),
(9, 8, '工单列表', 'ORDER_LIST', 'menu', '/order/list', 'list', 1, 1);

-- 初始测试号码
INSERT INTO phone_number (id, phone_number, status, remark) VALUES
(1, '13800138001', 'UNASSIGNED', '测试号码1'),
(2, '13800138002', 'UNASSIGNED', '测试号码2'),
(3, '13800138003', 'ASSIGNED', '测试号码3'),
(4, '13800138004', 'IN_USE', '测试号码4'),
(5, '13800138005', 'SUSPENDED', '测试号码5');
