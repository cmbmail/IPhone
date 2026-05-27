-- V62: Add phone:branch-assign and phone:branch-revoke permissions
INSERT INTO sys_permission (code, name, module, sort_order, created_at) VALUES
('phone:branch-assign', '分配号码到分行', 'phone', 6, NOW()),
('phone:branch-revoke', '从分行回收号码', 'phone', 7, NOW());

-- Assign new permissions to admin role (role_id=1)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE code IN ('phone:branch-assign', 'phone:branch-revoke');

-- Assign new permissions to ops role (role_id=2)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE code IN ('phone:branch-assign', 'phone:branch-revoke');
