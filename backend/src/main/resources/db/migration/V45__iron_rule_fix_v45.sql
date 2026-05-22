-- V45: Iron Rule 4 - Convert varchar status/type fields to TINYINT

-- 1. bill_raw.status: varchar → tinyint (0:待处理 1:已处理 2:错误) - same as import_status
-- Drop the duplicate varchar status column (import_status TINYINT already exists)
ALTER TABLE bill_raw DROP COLUMN status;

-- 2. bill_raw.charge_type: varchar → tinyint (1:月租 2:通话 3:短信 4:流量 5:其他)
ALTER TABLE bill_raw ADD COLUMN charge_type_new TINYINT DEFAULT 5 COMMENT '1:月租 2:通话 3:短信 4:流量 5:其他' AFTER charge_amount;
UPDATE bill_raw SET charge_type_new = CASE
    WHEN charge_type = 'monthly_rent' OR charge_type = '月租' THEN 1
    WHEN charge_type = 'call' OR charge_type = '通话' THEN 2
    WHEN charge_type = 'sms' OR charge_type = '短信' THEN 3
    WHEN charge_type = 'data' OR charge_type = '流量' THEN 4
    ELSE 5
END;
ALTER TABLE bill_raw DROP COLUMN charge_type;
ALTER TABLE bill_raw CHANGE COLUMN charge_type_new charge_type TINYINT DEFAULT 5 COMMENT '1:月租 2:通话 3:短信 4:流量 5:其他';

-- 3. import_batch.import_type: varchar → tinyint (1:号码 2:账单 3:设备 4:员工 5:发票 6:其他)
ALTER TABLE import_batch ADD COLUMN import_type_new TINYINT DEFAULT 6 COMMENT '1:号码 2:账单 3:设备 4:员工 5:发票 6:其他' AFTER batch_id;
UPDATE import_batch SET import_type_new = CASE
    WHEN import_type = 'phone' OR import_type = '号码' THEN 1
    WHEN import_type = 'bill' OR import_type = '账单' THEN 2
    WHEN import_type = 'device' OR import_type = '设备' THEN 3
    WHEN import_type = 'employee' OR import_type = '员工' THEN 4
    WHEN import_type = 'invoice' OR import_type = '发票' THEN 5
    ELSE 6
END;
ALTER TABLE import_batch DROP COLUMN import_type;
ALTER TABLE import_batch CHANGE COLUMN import_type_new import_type TINYINT DEFAULT 6 COMMENT '1:号码 2:账单 3:设备 4:员工 5:发票 6:其他';

-- 4. notification: drop zombie varchar columns (notification_type TINYINT already exists)
ALTER TABLE notification DROP COLUMN type;
ALTER TABLE notification DROP COLUMN priority;

-- 5. phone_device_history.from_status/to_status: varchar → tinyint
-- (0:库存 1:在用 2:停用 3:维修中 4:报废 - same as PhoneDevice.status)
ALTER TABLE phone_device_history ADD COLUMN from_status_new TINYINT COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废' AFTER action;
ALTER TABLE phone_device_history ADD COLUMN to_status_new TINYINT COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废' AFTER from_status_new;
UPDATE phone_device_history SET from_status_new = CASE
    WHEN from_status = 'stock' OR from_status = '库存' THEN 0
    WHEN from_status = 'in_use' OR from_status = 'active' OR from_status = '在用' THEN 1
    WHEN from_status = 'disabled' OR from_status = '停用' THEN 2
    WHEN from_status = 'repairing' OR from_status = '维修中' THEN 3
    WHEN from_status = 'retired' OR from_status = '报废' THEN 4
    ELSE NULL
END;
UPDATE phone_device_history SET to_status_new = CASE
    WHEN to_status = 'stock' OR to_status = '库存' THEN 0
    WHEN to_status = 'in_use' OR to_status = 'active' OR to_status = '在用' THEN 1
    WHEN to_status = 'disabled' OR to_status = '停用' THEN 2
    WHEN to_status = 'repairing' OR to_status = '维修中' THEN 3
    WHEN to_status = 'retired' OR to_status = '报废' THEN 4
    ELSE NULL
END;
ALTER TABLE phone_device_history DROP COLUMN from_status;
ALTER TABLE phone_device_history DROP COLUMN to_status;
ALTER TABLE phone_device_history CHANGE COLUMN from_status_new from_status TINYINT COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废';
ALTER TABLE phone_device_history CHANGE COLUMN to_status_new to_status TINYINT COMMENT '0:库存 1:在用 2:停用 3:维修中 4:报废';

-- 6. phone_history.from_status/to_status: varchar → tinyint
-- (0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用 - same as PhoneNumber.status)
ALTER TABLE phone_history ADD COLUMN from_status_new TINYINT COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用' AFTER action;
ALTER TABLE phone_history ADD COLUMN to_status_new TINYINT COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用' AFTER from_status_new;
UPDATE phone_history SET from_status_new = CASE
    WHEN from_status = 'idle' OR from_status = '空闲' THEN 0
    WHEN from_status = 'active' OR from_status = 'in_use' OR from_status = '在用' THEN 1
    WHEN from_status = 'suspended' OR from_status = '停机' THEN 2
    WHEN from_status = 'cancelled' OR from_status = '注销' THEN 3
    WHEN from_status = 'reserved' OR from_status = '预留' THEN 4
    WHEN from_status = 'disabled' OR from_status = '禁用' THEN 5
    ELSE NULL
END;
UPDATE phone_history SET to_status_new = CASE
    WHEN to_status = 'idle' OR to_status = '空闲' THEN 0
    WHEN to_status = 'active' OR to_status = 'in_use' OR to_status = '在用' THEN 1
    WHEN to_status = 'suspended' OR to_status = '停机' THEN 2
    WHEN to_status = 'cancelled' OR to_status = '注销' THEN 3
    WHEN to_status = 'reserved' OR to_status = '预留' THEN 4
    WHEN to_status = 'disabled' OR to_status = '禁用' THEN 5
    ELSE NULL
END;
ALTER TABLE phone_history DROP COLUMN from_status;
ALTER TABLE phone_history DROP COLUMN to_status;
ALTER TABLE phone_history CHANGE COLUMN from_status_new from_status TINYINT COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用';
ALTER TABLE phone_history CHANGE COLUMN to_status_new to_status TINYINT COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用';

-- 7. phone_snapshot.status: varchar → tinyint (same as PhoneNumber.status)
ALTER TABLE phone_snapshot ADD COLUMN status_new TINYINT NOT NULL DEFAULT 0 COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用' AFTER extension;
UPDATE phone_snapshot SET status_new = CASE
    WHEN status = 'idle' OR status = '空闲' THEN 0
    WHEN status = 'active' OR status = 'in_use' OR status = '在用' THEN 1
    WHEN status = 'suspended' OR status = '停机' THEN 2
    WHEN status = 'cancelled' OR status = '注销' THEN 3
    WHEN status = 'reserved' OR status = '预留' THEN 4
    WHEN status = 'disabled' OR status = '禁用' THEN 5
    ELSE 0
END;
ALTER TABLE phone_snapshot DROP COLUMN status;
ALTER TABLE phone_snapshot CHANGE COLUMN status_new status TINYINT NOT NULL DEFAULT 0 COMMENT '0:空闲 1:在用 2:停机 3:注销 4:预留 5:禁用';

-- 8. phone_surrender_record.surrender_type: varchar → tinyint (1:拆机 2:取消)
ALTER TABLE phone_surrender_record ADD COLUMN surrender_type_new TINYINT NOT NULL DEFAULT 1 COMMENT '1:拆机 2:取消' AFTER surrender_date;
UPDATE phone_surrender_record SET surrender_type_new = CASE
    WHEN surrender_type = 'surrender' OR surrender_type = '拆机' THEN 1
    WHEN surrender_type = 'cancel' OR surrender_type = '取消' THEN 2
    ELSE 1
END;
ALTER TABLE phone_surrender_record DROP COLUMN surrender_type;
ALTER TABLE phone_surrender_record CHANGE COLUMN surrender_type_new surrender_type TINYINT NOT NULL DEFAULT 1 COMMENT '1:拆机 2:取消';

-- 9. sys_feature_flag.scope_type: varchar → tinyint (1:ALL 2:ORGANIZATION 3:USER 4:CUSTOM)
ALTER TABLE sys_feature_flag ADD COLUMN scope_type_new TINYINT COMMENT '1:ALL 2:ORGANIZATION 3:USER 4:CUSTOM' AFTER is_enabled;
UPDATE sys_feature_flag SET scope_type_new = CASE
    WHEN scope_type = 'ALL' THEN 1
    WHEN scope_type = 'ORGANIZATION' THEN 2
    WHEN scope_type = 'USER' THEN 3
    WHEN scope_type = 'CUSTOM' THEN 4
    ELSE NULL
END;
ALTER TABLE sys_feature_flag DROP COLUMN scope_type;
ALTER TABLE sys_feature_flag CHANGE COLUMN scope_type_new scope_type TINYINT COMMENT '1:ALL 2:ORGANIZATION 3:USER 4:CUSTOM';

-- 10. subsidiary_reconciliation: enum ReconciliationStatus → tinyint (already has reconciliation_status TINYINT column)
-- The entity still has Java enum, need to fix in Java code

-- 11. invoice_distribution: enum DistributionStatus → tinyint (already has distribution_status TINYINT column)
-- The column is TINYINT but entity uses enum, need to fix in Java code
