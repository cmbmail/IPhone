-- V42: ENUM → TINYINT + 注释枚举值
-- 铁律4: 状态字段用 TINYINT + 注释枚举值
-- 策略: ADD新列 → 迁移数据 → DROP旧列 → RENAME新列

-- ===== announcement =====
ALTER TABLE announcement ADD COLUMN _announcement_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:系统 2:维护 3:政策 4:运营 5:其他';
UPDATE announcement SET _announcement_type = CASE announcement_type WHEN 'SYSTEM' THEN 1 WHEN 'MAINTENANCE' THEN 2 WHEN 'POLICY' THEN 3 WHEN 'OPERATION' THEN 4 WHEN 'OTHER' THEN 5 END;
ALTER TABLE announcement DROP COLUMN announcement_type;
ALTER TABLE announcement CHANGE _announcement_type announcement_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:系统 2:维护 3:政策 4:运营 5:其他';

ALTER TABLE announcement ADD COLUMN _priority TINYINT NOT NULL DEFAULT 3 COMMENT '1:紧急 2:高 3:普通 4:低';
UPDATE announcement SET _priority = CASE priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'NORMAL' THEN 3 WHEN 'LOW' THEN 4 END;
ALTER TABLE announcement DROP COLUMN priority;
ALTER TABLE announcement CHANGE _priority priority TINYINT NOT NULL DEFAULT 3 COMMENT '1:紧急 2:高 3:普通 4:低';

ALTER TABLE announcement ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:草稿 1:已发布 2:已归档';
UPDATE announcement SET _status = CASE status WHEN 'DRAFT' THEN 0 WHEN 'PUBLISHED' THEN 1 WHEN 'ARCHIVED' THEN 2 END;
ALTER TABLE announcement DROP COLUMN status;
ALTER TABLE announcement CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:草稿 1:已发布 2:已归档';

-- ===== bill_allocation =====
ALTER TABLE bill_allocation ADD COLUMN _admin_confirm_org TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:正确 2:错误';
UPDATE bill_allocation SET _admin_confirm_org = CASE admin_confirm_org WHEN 'pending' THEN 0 WHEN 'correct' THEN 1 WHEN 'wrong' THEN 2 END;
ALTER TABLE bill_allocation DROP COLUMN admin_confirm_org;
ALTER TABLE bill_allocation CHANGE _admin_confirm_org admin_confirm_org TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:正确 2:错误';

ALTER TABLE bill_allocation ADD COLUMN _admin_confirm_amount TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:正确 2:错误';
UPDATE bill_allocation SET _admin_confirm_amount = CASE admin_confirm_amount WHEN 'pending' THEN 0 WHEN 'correct' THEN 1 WHEN 'wrong' THEN 2 END;
ALTER TABLE bill_allocation DROP COLUMN admin_confirm_amount;
ALTER TABLE bill_allocation CHANGE _admin_confirm_amount admin_confirm_amount TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:正确 2:错误';

ALTER TABLE bill_allocation ADD COLUMN _finance_confirm_anomaly TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:已确认 2:已驳回';
UPDATE bill_allocation SET _finance_confirm_anomaly = CASE finance_confirm_anomaly WHEN 'pending' THEN 0 WHEN 'confirmed' THEN 1 WHEN 'rejected' THEN 2 END;
ALTER TABLE bill_allocation DROP COLUMN finance_confirm_anomaly;
ALTER TABLE bill_allocation CHANGE _finance_confirm_anomaly finance_confirm_anomaly TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:已确认 2:已驳回';

ALTER TABLE bill_allocation ADD COLUMN _finance_confirm_submit TINYINT NOT NULL DEFAULT 0 COMMENT '0:待提交 1:已提交';
UPDATE bill_allocation SET _finance_confirm_submit = CASE finance_confirm_submit WHEN 'pending' THEN 0 WHEN 'submitted' THEN 1 END;
ALTER TABLE bill_allocation DROP COLUMN finance_confirm_submit;
ALTER TABLE bill_allocation CHANGE _finance_confirm_submit finance_confirm_submit TINYINT NOT NULL DEFAULT 0 COMMENT '0:待提交 1:已提交';

-- ===== bill_raw =====
ALTER TABLE bill_raw ADD COLUMN _import_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:已处理 2:错误';
UPDATE bill_raw SET _import_status = CASE import_status WHEN 'pending' THEN 0 WHEN 'processed' THEN 1 WHEN 'error' THEN 2 END;
ALTER TABLE bill_raw DROP COLUMN import_status;
ALTER TABLE bill_raw CHANGE _import_status import_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:已处理 2:错误';

-- ===== cost_center_mapping =====
ALTER TABLE cost_center_mapping ADD COLUMN _status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';
UPDATE cost_center_mapping SET _status = CASE status WHEN 'active' THEN 1 WHEN 'inactive' THEN 0 END;
ALTER TABLE cost_center_mapping DROP COLUMN status;
ALTER TABLE cost_center_mapping CHANGE _status status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';

-- ===== device =====
ALTER TABLE device ADD COLUMN _device_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:IP话机 2:软电话 3:ATA 4:网关';
UPDATE device SET _device_type = CASE device_type WHEN 'IP_PHONE' THEN 1 WHEN 'SOFT_PHONE' THEN 2 WHEN 'ATA' THEN 3 WHEN 'GATEWAY' THEN 4 END;
ALTER TABLE device DROP COLUMN device_type;
ALTER TABLE device CHANGE _device_type device_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:IP话机 2:软电话 3:ATA 4:网关';

ALTER TABLE device ADD COLUMN _status TINYINT NOT NULL DEFAULT 3 COMMENT '1:在线 2:离线 3:未注册 4:已禁用';
UPDATE device SET _status = CASE status WHEN 'ONLINE' THEN 1 WHEN 'OFFLINE' THEN 2 WHEN 'UNREGISTERED' THEN 3 WHEN 'DISABLED' THEN 4 END;
ALTER TABLE device DROP COLUMN status;
ALTER TABLE device CHANGE _status status TINYINT NOT NULL DEFAULT 3 COMMENT '1:在线 2:离线 3:未注册 4:已禁用';

-- ===== device_operation =====
ALTER TABLE device_operation ADD COLUMN _operation_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:重启 2:配置同步 3:固件升级 4:恢复出厂 5:注册';
UPDATE device_operation SET _operation_type = CASE operation_type WHEN 'REBOOT' THEN 1 WHEN 'CONFIG_SYNC' THEN 2 WHEN 'FIRMWARE_UPGRADE' THEN 3 WHEN 'FACTORY_RESET' THEN 4 WHEN 'REGISTER' THEN 5 END;
ALTER TABLE device_operation DROP COLUMN operation_type;
ALTER TABLE device_operation CHANGE _operation_type operation_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:重启 2:配置同步 3:固件升级 4:恢复出厂 5:注册';

ALTER TABLE device_operation ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败';
UPDATE device_operation SET _status = CASE status WHEN 'PENDING' THEN 0 WHEN 'PROCESSING' THEN 1 WHEN 'COMPLETED' THEN 2 WHEN 'FAILED' THEN 3 END;
ALTER TABLE device_operation DROP COLUMN status;
ALTER TABLE device_operation CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败';

-- ===== employee =====
ALTER TABLE employee ADD COLUMN _status TINYINT NOT NULL DEFAULT 1 COMMENT '1:在职 0:离职';
UPDATE employee SET _status = CASE status WHEN 'active' THEN 1 WHEN 'inactive' THEN 0 END;
ALTER TABLE employee DROP COLUMN status;
ALTER TABLE employee CHANGE _status status TINYINT NOT NULL DEFAULT 1 COMMENT '1:在职 0:离职';

-- ===== extension_number =====
ALTER TABLE extension_number ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:可分配 1:已占用 2:闲置(无电话)';
UPDATE extension_number SET _status = CASE status WHEN 'AVAILABLE' THEN 0 WHEN 'ALLOCATED' THEN 1 WHEN 'IDLE' THEN 2 END;
ALTER TABLE extension_number DROP COLUMN status;
ALTER TABLE extension_number CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:可分配 1:已占用 2:闲置(无电话)';

-- ===== import_batch =====
ALTER TABLE import_batch ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败';
UPDATE import_batch SET _status = CASE status WHEN 'PENDING' THEN 0 WHEN 'PROCESSING' THEN 1 WHEN 'COMPLETED' THEN 2 WHEN 'FAILED' THEN 3 END;
ALTER TABLE import_batch DROP COLUMN status;
ALTER TABLE import_batch CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败';

-- ===== invoice =====
ALTER TABLE invoice ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待分配 1:已分发 2:已读 3:已确认';
UPDATE invoice SET _status = CASE status WHEN 'pending' THEN 0 WHEN 'distributed' THEN 1 WHEN 'read' THEN 2 WHEN 'confirmed' THEN 3 END;
ALTER TABLE invoice DROP COLUMN status;
ALTER TABLE invoice CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待分配 1:已分发 2:已读 3:已确认';

-- ===== invoice_distribution =====
ALTER TABLE invoice_distribution ADD COLUMN _distribution_status TINYINT NOT NULL DEFAULT 1 COMMENT '0:失败 1:成功';
UPDATE invoice_distribution SET _distribution_status = CASE distribution_status WHEN 'failed' THEN 0 WHEN 'success' THEN 1 END;
ALTER TABLE invoice_distribution DROP COLUMN distribution_status;
ALTER TABLE invoice_distribution CHANGE _distribution_status distribution_status TINYINT NOT NULL DEFAULT 1 COMMENT '0:失败 1:成功';

-- ===== notification =====
ALTER TABLE notification ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:未读 1:已读 2:已归档';
UPDATE notification SET _status = CASE status WHEN 'UNREAD' THEN 0 WHEN 'READ' THEN 1 WHEN 'ARCHIVED' THEN 2 END;
ALTER TABLE notification DROP COLUMN status;
ALTER TABLE notification CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:未读 1:已读 2:已归档';

ALTER TABLE notification ADD COLUMN _notification_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码分配 2:号码交回 3:号码状态变更 4:设备离线 5:设备上线 6:账单逾期 7:系统告警 8:工单指派 9:导入完成';
UPDATE notification SET _notification_type = CASE notification_type WHEN 'PHONE_ALLOCATED' THEN 1 WHEN 'PHONE_SURRENDERED' THEN 2 WHEN 'PHONE_STATUS_CHANGED' THEN 3 WHEN 'DEVICE_OFFLINE' THEN 4 WHEN 'DEVICE_ONLINE' THEN 5 WHEN 'BILL_OVERDUE' THEN 6 WHEN 'SYSTEM_ALERT' THEN 7 WHEN 'WORK_ORDER_ASSIGNED' THEN 8 WHEN 'IMPORT_COMPLETED' THEN 9 END;
ALTER TABLE notification DROP COLUMN notification_type;
ALTER TABLE notification CHANGE _notification_type notification_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码分配 2:号码交回 3:号码状态变更 4:设备离线 5:设备上线 6:账单逾期 7:系统告警 8:工单指派 9:导入完成';

-- ===== org_structure =====
ALTER TABLE org_structure ADD COLUMN _type TINYINT NOT NULL DEFAULT 3 COMMENT '1:集团 2:子公司 3:部门';
UPDATE org_structure SET _type = CASE type WHEN 'group' THEN 1 WHEN 'subsidiary' THEN 2 WHEN 'dept' THEN 3 END;
ALTER TABLE org_structure DROP COLUMN type;
ALTER TABLE org_structure CHANGE _type type TINYINT NOT NULL DEFAULT 3 COMMENT '1:集团 2:子公司 3:部门';

ALTER TABLE org_structure ADD COLUMN _status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';
UPDATE org_structure SET _status = CASE status WHEN 'active' THEN 1 WHEN 'inactive' THEN 0 END;
ALTER TABLE org_structure DROP COLUMN status;
ALTER TABLE org_structure CHANGE _status status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';

-- ===== phone_device =====
ALTER TABLE phone_device ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废';
UPDATE phone_device SET _status = CASE status WHEN 'stock' THEN 0 WHEN 'active' THEN 1 WHEN 'inactive' THEN 2 WHEN 'repairing' THEN 3 WHEN 'retired' THEN 4 END;
ALTER TABLE phone_device DROP COLUMN status;
ALTER TABLE phone_device CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废';

-- ===== phone_number =====
ALTER TABLE phone_number ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用';
UPDATE phone_number SET _status = CASE status WHEN 'idle' THEN 0 WHEN 'active' THEN 1 WHEN 'stopped' THEN 2 WHEN 'cancelled' THEN 3 WHEN 'reserved' THEN 4 WHEN 'disabled' THEN 5 END;
ALTER TABLE phone_number DROP COLUMN status;
ALTER TABLE phone_number CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用';

-- ===== subsidiary_reconciliation =====
ALTER TABLE subsidiary_reconciliation ADD COLUMN _reconciliation_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:子公司已确认 2:集团已确认';
UPDATE subsidiary_reconciliation SET _reconciliation_status = CASE reconciliation_status WHEN 'pending' THEN 0 WHEN 'confirmed_by_subsidiary' THEN 1 WHEN 'confirmed_by_group' THEN 2 END;
ALTER TABLE subsidiary_reconciliation DROP COLUMN reconciliation_status;
ALTER TABLE subsidiary_reconciliation CHANGE _reconciliation_status reconciliation_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待确认 1:子公司已确认 2:集团已确认';

-- ===== sys_role =====
ALTER TABLE sys_role ADD COLUMN _status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';
UPDATE sys_role SET _status = CASE status WHEN 'active' THEN 1 WHEN 'inactive' THEN 0 END;
ALTER TABLE sys_role DROP COLUMN status;
ALTER TABLE sys_role CHANGE _status status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';

-- ===== sys_user =====
ALTER TABLE sys_user ADD COLUMN _role TINYINT NOT NULL DEFAULT 1 COMMENT '1:管理员 2:运维 3:财务 4:领导';
UPDATE sys_user SET _role = CASE role WHEN 'admin' THEN 1 WHEN 'ops' THEN 2 WHEN 'finance' THEN 3 WHEN 'boss' THEN 4 END;
ALTER TABLE sys_user DROP COLUMN role;
ALTER TABLE sys_user CHANGE _role role TINYINT NOT NULL DEFAULT 1 COMMENT '1:管理员 2:运维 3:财务 4:领导';

ALTER TABLE sys_user ADD COLUMN _status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';
UPDATE sys_user SET _status = CASE status WHEN 'active' THEN 1 WHEN 'inactive' THEN 0 END;
ALTER TABLE sys_user DROP COLUMN status;
ALTER TABLE sys_user CHANGE _status status TINYINT NOT NULL DEFAULT 1 COMMENT '0:停用 1:启用';

-- ===== work_order =====
ALTER TABLE work_order ADD COLUMN _type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码分配 2:号码转移 3:号码变更 4:组织变更 5:号码回收 6:号码交回 7:号码启用 8:号码停用';
UPDATE work_order SET _type = CASE type WHEN 'PHONE_ALLOCATE' THEN 1 WHEN 'PHONE_TRANSFER' THEN 2 WHEN 'PHONE_CHANGE_NUMBER' THEN 3 WHEN 'PHONE_CHANGE_ORG' THEN 4 WHEN 'PHONE_RECLAIM' THEN 5 WHEN 'PHONE_SURRENDER' THEN 6 WHEN 'PHONE_ENABLE' THEN 7 WHEN 'PHONE_DISABLE' THEN 8 END;
ALTER TABLE work_order DROP COLUMN type;
ALTER TABLE work_order CHANGE _type type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码分配 2:号码转移 3:号码变更 4:组织变更 5:号码回收 6:号码交回 7:号码启用 8:号码停用';

ALTER TABLE work_order ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:已接受 2:处理中 3:已完成 4:已归档 5:已取消';
UPDATE work_order SET _status = CASE status WHEN 'pending' THEN 0 WHEN 'accepted' THEN 1 WHEN 'processing' THEN 2 WHEN 'completed' THEN 3 WHEN 'archived' THEN 4 WHEN 'cancelled' THEN 5 END;
ALTER TABLE work_order DROP COLUMN status;
ALTER TABLE work_order CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:已接受 2:处理中 3:已完成 4:已归档 5:已取消';

ALTER TABLE work_order ADD COLUMN _priority TINYINT NOT NULL DEFAULT 2 COMMENT '1:低 2:普通 3:高 4:紧急';
UPDATE work_order SET _priority = CASE priority WHEN 'low' THEN 1 WHEN 'normal' THEN 2 WHEN 'high' THEN 3 WHEN 'urgent' THEN 4 END;
ALTER TABLE work_order DROP COLUMN priority;
ALTER TABLE work_order CHANGE _priority priority TINYINT NOT NULL DEFAULT 2 COMMENT '1:低 2:普通 3:高 4:紧急';

-- ===== work_order_item =====
ALTER TABLE work_order_item ADD COLUMN _item_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码 2:设备 3:员工';
UPDATE work_order_item SET _item_type = CASE item_type WHEN 'phone' THEN 1 WHEN 'device' THEN 2 WHEN 'employee' THEN 3 END;
ALTER TABLE work_order_item DROP COLUMN item_type;
ALTER TABLE work_order_item CHANGE _item_type item_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:号码 2:设备 3:员工';

ALTER TABLE work_order_item ADD COLUMN _status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败 4:跳过';
UPDATE work_order_item SET _status = CASE status WHEN 'pending' THEN 0 WHEN 'processing' THEN 1 WHEN 'completed' THEN 2 WHEN 'failed' THEN 3 WHEN 'skipped' THEN 4 END;
ALTER TABLE work_order_item DROP COLUMN status;
ALTER TABLE work_order_item CHANGE _status status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:处理中 2:已完成 3:失败 4:跳过';
