-- V32: Extend sys_permission with missing codes
INSERT IGNORE INTO sys_permission (code, name, module, description) VALUES
('org:import', '导入组织', 'org', 'Import organizations from Excel'),
('bill:delete', '删除账单', 'bill', 'Delete bill records'),
('bill:confirm', '确认账单分摊', 'bill', 'Confirm bill allocation'),
('bill:submit', '提交账单', 'bill', 'Submit bill for approval'),
('bill:reject', '驳回账单', 'bill', 'Reject bill allocation'),
('device:checkin', '设备签到', 'device', 'Device check-in'),
('device:operate', '设备操作', 'device', 'Execute device operations'),
('inv:delete', '删除发票', 'inv', 'Delete invoices'),
('recon:generate', '生成对账单', 'recon', 'Generate reconciliation'),
('recon:confirm', '确认对账', 'recon', 'Confirm reconciliation'),
('wo:accept', '接单', 'wo', 'Accept work order'),
('wo:process', '处理工单', 'wo', 'Process work order'),
('wo:complete', '完成工单', 'wo', 'Complete work order'),
('wo:reject', '驳回工单', 'wo', 'Reject work order'),
('phone:import', '导入号码', 'phone', 'Import phone numbers'),
('phone:surrender', '交回号码', 'phone', 'Surrender phone number'),
('phone:reserve', '预留号码', 'phone', 'Reserve phone number'),
('phone:change', '变更号码', 'phone', 'Change phone assignment'),
('sys:reset-password', '重置密码', 'sys', 'Reset user password'),
('sys:disable-user', '禁用用户', 'sys', 'Disable user account'),
('sys:enable-user', '启用用户', 'sys', 'Enable user account'),
('sys:delete-user', '删除用户', 'sys', 'Delete user account');

-- Assign all new permissions to admin role (role_id=1)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE code IN (
    'org:import', 'bill:delete', 'bill:confirm', 'bill:submit', 'bill:reject',
    'device:checkin', 'device:operate', 'inv:delete', 'recon:generate', 'recon:confirm',
    'wo:accept', 'wo:process', 'wo:complete', 'wo:reject',
    'phone:import', 'phone:surrender', 'phone:reserve', 'phone:change',
    'sys:reset-password', 'sys:disable-user', 'sys:enable-user', 'sys:delete-user'
);

-- Assign phone/device/wo permissions to ops role (role_id=2)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE code IN (
    'phone:view', 'phone:assign', 'phone:revoke', 'phone:import', 'phone:surrender', 'phone:reserve', 'phone:change',
    'device:view', 'device:assign', 'device:revoke', 'device:checkin', 'device:operate',
    'wo:view', 'wo:create', 'wo:edit', 'wo:delete', 'wo:accept', 'wo:process', 'wo:complete', 'wo:reject',
    'ext:view', 'areacode:view'
);

-- Assign bill/inv/recon permissions to finance role (role_id=3)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE code IN (
    'bill:view', 'bill:import', 'bill:allocate', 'bill:confirm', 'bill:submit', 'bill:reject', 'bill:delete',
    'inv:view', 'inv:create', 'inv:edit', 'inv:delete',
    'recon:view', 'recon:generate', 'recon:confirm',
    'cost:view', 'rpt:view'
);

-- Assign rpt/bill:view to boss role (role_id=4)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE code IN (
    'rpt:view', 'bill:view', 'inv:view', 'recon:view', 'cost:view',
    'org:view', 'emp:view', 'phone:view', 'device:view'
);
