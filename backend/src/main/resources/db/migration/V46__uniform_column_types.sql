-- =============================================================
-- V46: 统一同名列的列类型（铁律补充：跨表同名列类型必须一致）
-- =============================================================

-- ===== P0: 类型根本不同的列 =====

-- 1. device_id: Device/DeviceOperation是varchar(50)业务编号, DevicePhoneMapping/PhoneDeviceHistory是bigint FK
--    这不是同一个概念，只是同名。重命名FK列以消除歧义
ALTER TABLE device_phone_mapping CHANGE COLUMN device_id phone_device_id BIGINT NOT NULL;
ALTER TABLE phone_device_history CHANGE COLUMN device_id phone_device_id BIGINT NOT NULL;

-- 2. user_id: Notification是bigint(系统用户ID), BillRaw/PhoneNumber是varchar(20)(员工编号)
--    语义不同，重命名Notification的列
ALTER TABLE notification CHANGE COLUMN user_id sys_user_id BIGINT NOT NULL;

-- 3. target_id: AuditLogEntity是varchar(100)多态, WorkOrderItem是bigint FK
--    语义不同，重命名
ALTER TABLE work_order_item CHANGE COLUMN target_id target_ref_id BIGINT;

-- ===== P1: varchar长度不一致 → 取最大值统一 =====

-- action: varchar(30) vs varchar(50) → 统一50
ALTER TABLE phone_device_history MODIFY COLUMN action VARCHAR(50) NOT NULL;
ALTER TABLE phone_history MODIFY COLUMN action VARCHAR(50) NOT NULL;

-- batch_id: varchar(32) vs varchar(50) → 统一50
ALTER TABLE import_batch MODIFY COLUMN batch_id VARCHAR(50) NOT NULL;

-- code: varchar(50) vs varchar(100) → 统一100
ALTER TABLE sys_role MODIFY COLUMN code VARCHAR(100) NOT NULL;

-- employee_no: varchar(20) vs varchar(50) → 统一50 (phone_snapshot可能存长编号)
ALTER TABLE employee MODIFY COLUMN employee_no VARCHAR(50) NOT NULL;
ALTER TABLE sys_user MODIFY COLUMN employee_no VARCHAR(50) NOT NULL;

-- extension_number: varchar(10) vs varchar(20) → 统一20
ALTER TABLE phone_number MODIFY COLUMN extension_number VARCHAR(20);

-- mac_address: varchar(12) vs varchar(20) → 统一20
--    Device.mac_address可能是带分隔符的格式(XX:XX:XX:XX:XX:XX=17字符)
--    PhoneDevice/PhoneDeviceHistory是去分隔符的(XXXXXXXXXXXX=12字符)
--    统一20以兼容两种格式
ALTER TABLE phone_device MODIFY COLUMN mac_address VARCHAR(20) NOT NULL;
ALTER TABLE phone_device_history MODIFY COLUMN mac_address VARCHAR(20) NOT NULL;

-- name: varchar(50) vs varchar(100) → 统一100
ALTER TABLE employee MODIFY COLUMN name VARCHAR(100) NOT NULL;
ALTER TABLE sys_role MODIFY COLUMN name VARCHAR(100);

-- operator: varchar(50) vs varchar(100) → 统一100
ALTER TABLE audit_log MODIFY COLUMN operator VARCHAR(100) NOT NULL;
ALTER TABLE phone_device_history MODIFY COLUMN operator VARCHAR(100) NOT NULL;
ALTER TABLE phone_history MODIFY COLUMN operator VARCHAR(100) NOT NULL;
ALTER TABLE phone_surrender_record MODIFY COLUMN operator VARCHAR(100) NOT NULL;

-- phone_number: varchar(20) vs varchar(50) → 统一50
--    bill_raw存原始导入数据，可能有格式前缀
ALTER TABLE bill_allocation MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;
ALTER TABLE device MODIFY COLUMN phone_number VARCHAR(50);
ALTER TABLE extension_number MODIFY COLUMN phone_number VARCHAR(50);
ALTER TABLE phone_history MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;
ALTER TABLE phone_number MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;
ALTER TABLE phone_ownership MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;
ALTER TABLE phone_snapshot MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;
ALTER TABLE phone_surrender_record MODIFY COLUMN phone_number VARCHAR(50) NOT NULL;

-- description: varchar(200) vs varchar(500) vs varchar(1000) → 按用途保留，不统一
--    flyway_schema_history是框架表不改；sys_role(200)/sys_feature_flag(500)/work_order(1000)各有用途

-- error_message: text vs varchar(500) → 统一TEXT
ALTER TABLE device_operation MODIFY COLUMN error_message TEXT;
ALTER TABLE work_order_item MODIFY COLUMN error_message TEXT;

-- created_by / updated_by: varchar(50) vs varchar(255) → 统一50
--    work_order_item是异常值，改回50
ALTER TABLE work_order_item MODIFY COLUMN created_by VARCHAR(50) NOT NULL;
ALTER TABLE work_order_item MODIFY COLUMN updated_by VARCHAR(50) NOT NULL;

-- ===== P1: priority int vs tinyint → 统一tinyint =====
ALTER TABLE area_code_org_mapping MODIFY COLUMN priority TINYINT NOT NULL DEFAULT 1;

-- ===== P1: datetime(6) vs datetime → 统一datetime =====
ALTER TABLE subsidiary_reconciliation MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE subsidiary_reconciliation MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ===== P1: version int vs bigint → 统一bigint (JPA @Version标准) =====
ALTER TABLE announcement MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE phone_device MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
