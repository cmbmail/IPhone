-- 创建数据库
CREATE DATABASE IF NOT EXISTS phonebiz DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE phonebiz;

-- 组织架构表
CREATE TABLE IF NOT EXISTS sys_org (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父组织ID',
    org_name VARCHAR(100) NOT NULL COMMENT '组织名称',
    org_code VARCHAR(50) NOT NULL COMMENT '组织编码',
    org_level INT DEFAULT 1 COMMENT '组织层级',
    sort INT DEFAULT 0 COMMENT '排序',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构表';

-- 员工表
CREATE TABLE IF NOT EXISTS sys_employee (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT COMMENT '组织ID',
    emp_no VARCHAR(50) NOT NULL COMMENT '工号',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    gender INT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    id_card VARCHAR(18) COMMENT '身份证号',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    menu_code VARCHAR(50) NOT NULL COMMENT '菜单编码',
    menu_type VARCHAR(20) COMMENT '菜单类型',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '组件路径',
    icon VARCHAR(100) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 员工角色关联表
CREATE TABLE IF NOT EXISTS sys_employee_role (
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (employee_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工角色关联表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 电话号码表
CREATE TABLE IF NOT EXISTS phone_number (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    phone_number VARCHAR(20) NOT NULL UNIQUE COMMENT '电话号码',
    status VARCHAR(20) DEFAULT 'UNASSIGNED' COMMENT '状态',
    employee_id BIGINT COMMENT '员工ID',
    employee_name VARCHAR(50) COMMENT '员工姓名',
    remark VARCHAR(500) COMMENT '备注',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电话号码表';

-- 工单表
CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '工单号',
    order_type VARCHAR(20) COMMENT '工单类型',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    phone_id BIGINT COMMENT '电话号码ID',
    phone_number VARCHAR(20) COMMENT '电话号码',
    requester_id BIGINT COMMENT '申请人ID',
    requester_name VARCHAR(50) COMMENT '申请人姓名',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(50) COMMENT '处理人姓名',
    title VARCHAR(100) COMMENT '工单标题',
    content TEXT COMMENT '工单内容',
    reason VARCHAR(500) COMMENT '原因',
    result VARCHAR(500) COMMENT '处理结果',
    priority INT DEFAULT 1 COMMENT '优先级 1普通 2紧急 3加急',
    create_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标识 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 插入初始数据
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
