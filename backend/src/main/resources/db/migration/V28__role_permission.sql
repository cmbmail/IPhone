-- V28: Role-Based Access Control
-- sys_role, sys_permission, sys_role_permission, sys_user.role_id

-- 1. Roles table
CREATE TABLE sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'active',
    is_system   TINYINT(1)   NOT NULL DEFAULT 0,
    created_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(50)  NOT NULL DEFAULT 'system',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
);

-- 2. Permissions table
CREATE TABLE sys_permission (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    module     VARCHAR(50)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Role-Permission junction table
CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

-- 4. Add role_id FK to sys_user
ALTER TABLE sys_user ADD COLUMN role_id BIGINT NULL;
ALTER TABLE sys_user ADD CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE SET NULL;

-- 5. Seed system roles
INSERT INTO sys_role (name, code, description, is_system) VALUES
    ('系统管理员', 'admin', '拥有所有系统权限', 1),
    ('运维人员',   'ops',   '号码和设备运维管理', 1),
    ('财务人员',   'finance', '账单、发票和费用管理', 1),
    ('管理者',     'boss',  '报表查看和审批', 1);

-- 6. Seed permissions
INSERT INTO sys_permission (code, name, module, sort_order) VALUES
    -- 组织管理
    ('org:view',   '查看组织',   '组织管理', 1),
    ('org:create', '创建组织',   '组织管理', 2),
    ('org:edit',   '编辑组织',   '组织管理', 3),
    ('org:delete', '删除组织',   '组织管理', 4),
    ('org:import', '批量导入',   '组织管理', 5),
    -- 员工管理
    ('emp:view',   '查看员工',   '员工管理', 1),
    ('emp:create', '创建员工',   '员工管理', 2),
    ('emp:edit',   '编辑员工',   '员工管理', 3),
    ('emp:delete', '停用员工',   '员工管理', 4),
    -- 号码资源
    ('phone:view',     '查看号码',   '号码资源', 1),
    ('phone:assign',   '分配号码',   '号码资源', 2),
    ('phone:revoke',   '回收号码',   '号码资源', 3),
    ('device:view',    '查看设备',   '号码资源', 4),
    ('device:assign',  '分配设备',   '号码资源', 5),
    ('device:revoke',  '回收设备',   '号码资源', 6),
    ('ext:view',       '查看分机池', '号码资源', 7),
    ('areacode:view',  '查看区号',   '号码资源', 8),
    -- 费用管理
    ('bill:view',    '查看账单',   '费用管理', 1),
    ('bill:import',  '导入账单',   '费用管理', 2),
    ('bill:allocate','分摊账单',   '费用管理', 3),
    ('cost:view',    '查看成本中心','费用管理', 4),
    ('inv:view',     '查看发票',   '费用管理', 5),
    ('inv:create',   '创建发票',   '费用管理', 6),
    ('inv:edit',     '编辑发票',   '费用管理', 7),
    ('recon:view',   '子公司对账', '费用管理', 8),
    -- 工单管理
    ('wo:view',   '查看工单', '工单管理', 1),
    ('wo:create', '创建工单', '工单管理', 2),
    ('wo:edit',   '编辑工单', '工单管理', 3),
    ('wo:delete', '删除工单', '工单管理', 4),
    -- 报表中心
    ('rpt:view', '查看报表', '报表中心', 1),
    -- 系统管理
    ('sys:user',   '用户管理', '系统管理', 1),
    ('sys:role',   '角色管理', '系统管理', 2),
    ('sys:config', '系统配置', '系统管理', 3);

-- 7. Assign permissions to roles

-- admin: all permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- ops: org/employee view, phone/device/ext/areacode full, work order
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE code IN (
    'org:view', 'emp:view',
    'phone:view', 'phone:assign', 'phone:revoke',
    'device:view', 'device:assign', 'device:revoke',
    'ext:view', 'areacode:view',
    'wo:view', 'wo:create', 'wo:edit'
);

-- finance: org/employee view, bill/cost/inv/recon, report
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE code IN (
    'org:view', 'emp:view',
    'bill:view', 'bill:import', 'bill:allocate',
    'cost:view',
    'inv:view', 'inv:create', 'inv:edit',
    'recon:view',
    'rpt:view'
);

-- boss: read-only on most things
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE code IN (
    'org:view', 'emp:view',
    'phone:view',
    'bill:view',
    'inv:view',
    'wo:view',
    'rpt:view'
);

-- 8. Link existing sys_user records to sys_role
UPDATE sys_user SET role_id = 1 WHERE role = 'admin';
UPDATE sys_user SET role_id = 2 WHERE role = 'ops';
UPDATE sys_user SET role_id = 3 WHERE role = 'finance';
UPDATE sys_user SET role_id = 4 WHERE role = 'boss';
