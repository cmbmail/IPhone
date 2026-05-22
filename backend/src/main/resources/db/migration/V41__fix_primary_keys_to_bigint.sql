-- V41: 主键 bigint unsigned → bigint + 所有外键同步
-- 铁律1: 主键统一格式 id BIGINT AUTO_INCREMENT
-- 策略: 先删除FK约束 → 改列类型 → 重建FK约束

-- ===== 1. 删除外键约束 =====
ALTER TABLE area_code_org_mapping DROP FOREIGN KEY fk_area_org;
ALTER TABLE cost_center_mapping DROP FOREIGN KEY fk_cc_org;
ALTER TABLE employee DROP FOREIGN KEY fk_employee_org;
ALTER TABLE extension_pool DROP FOREIGN KEY fk_pool_org;
ALTER TABLE invoice DROP FOREIGN KEY invoice_ibfk_1;
ALTER TABLE invoice DROP FOREIGN KEY invoice_ibfk_2;
ALTER TABLE invoice_distribution DROP FOREIGN KEY invoice_distribution_ibfk_1;
ALTER TABLE invoice_file DROP FOREIGN KEY invoice_file_ibfk_1;
ALTER TABLE org_structure DROP FOREIGN KEY fk_org_parent;
ALTER TABLE phone_history DROP FOREIGN KEY fk_history_phone;
ALTER TABLE phone_number DROP FOREIGN KEY fk_phone_alloc_org;
ALTER TABLE phone_number DROP FOREIGN KEY fk_phone_org;
ALTER TABLE sys_user DROP FOREIGN KEY fk_user_employee;
ALTER TABLE sys_user DROP FOREIGN KEY fk_user_org;
ALTER TABLE sys_user DROP FOREIGN KEY fk_user_role;
ALTER TABLE work_order_item DROP FOREIGN KEY work_order_item_ibfk_1;

-- ===== 2. 改主键 =====
ALTER TABLE area_code_org_mapping MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE bill_allocation MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE bill_raw MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE cost_center_mapping MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE device_operation MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE device_phone_mapping MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE employee MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE extension_number MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE extension_pool MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE import_batch MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE invoice MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE invoice_distribution MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE invoice_file MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE notification MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE org_structure MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_device MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_device_history MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_history MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_number MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_ownership MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE phone_surrender_record MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE sys_user MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- ===== 3. 改外键列 =====
ALTER TABLE area_code_org_mapping MODIFY COLUMN org_id BIGINT NOT NULL;
ALTER TABLE bill_allocation MODIFY COLUMN bill_raw_id BIGINT NOT NULL;
ALTER TABLE bill_allocation MODIFY COLUMN phone_id BIGINT DEFAULT NULL;
ALTER TABLE bill_allocation MODIFY COLUMN snapshot_org_id BIGINT DEFAULT NULL;
ALTER TABLE cost_center_mapping MODIFY COLUMN org_id BIGINT NOT NULL;
ALTER TABLE device_phone_mapping MODIFY COLUMN device_id BIGINT NOT NULL;
ALTER TABLE device_phone_mapping MODIFY COLUMN phone_id BIGINT NOT NULL;
ALTER TABLE employee MODIFY COLUMN org_id BIGINT NOT NULL;
ALTER TABLE extension_number MODIFY COLUMN pool_id BIGINT NOT NULL;
ALTER TABLE extension_number MODIFY COLUMN dept_org_id BIGINT DEFAULT NULL;
ALTER TABLE extension_number MODIFY COLUMN phone_id BIGINT DEFAULT NULL;
ALTER TABLE extension_number MODIFY COLUMN work_order_id BIGINT DEFAULT NULL;
ALTER TABLE extension_pool MODIFY COLUMN org_id BIGINT NOT NULL;
ALTER TABLE invoice MODIFY COLUMN source_org_id BIGINT NOT NULL;
ALTER TABLE invoice MODIFY COLUMN recipient_org_id BIGINT NOT NULL;
ALTER TABLE invoice_distribution MODIFY COLUMN invoice_id BIGINT NOT NULL;
ALTER TABLE invoice_file MODIFY COLUMN invoice_id BIGINT NOT NULL;
ALTER TABLE notification MODIFY COLUMN source_id BIGINT DEFAULT NULL;
ALTER TABLE notification MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE org_structure MODIFY COLUMN parent_id BIGINT DEFAULT NULL;
ALTER TABLE phone_device MODIFY COLUMN org_id BIGINT NOT NULL;
ALTER TABLE phone_device_history MODIFY COLUMN device_id BIGINT NOT NULL;
ALTER TABLE phone_history MODIFY COLUMN phone_id BIGINT NOT NULL;
ALTER TABLE phone_number MODIFY COLUMN org_id BIGINT DEFAULT NULL;
ALTER TABLE phone_number MODIFY COLUMN allocation_org_id BIGINT DEFAULT NULL;
ALTER TABLE phone_ownership MODIFY COLUMN branch_org_id BIGINT DEFAULT NULL;
ALTER TABLE phone_ownership MODIFY COLUMN dept_org_id BIGINT DEFAULT NULL;
ALTER TABLE phone_snapshot MODIFY COLUMN phone_id BIGINT NOT NULL;
ALTER TABLE phone_snapshot MODIFY COLUMN org_id BIGINT DEFAULT NULL;
ALTER TABLE phone_surrender_record MODIFY COLUMN phone_id BIGINT NOT NULL;
ALTER TABLE subsidiary_reconciliation MODIFY COLUMN subsidiary_org_id BIGINT NOT NULL;
ALTER TABLE sys_role_permission MODIFY COLUMN role_id BIGINT NOT NULL;
ALTER TABLE sys_role_permission MODIFY COLUMN permission_id BIGINT NOT NULL;
ALTER TABLE sys_user MODIFY COLUMN scope_org_id BIGINT DEFAULT NULL;
ALTER TABLE sys_user MODIFY COLUMN role_id BIGINT DEFAULT NULL;
ALTER TABLE work_order MODIFY COLUMN requester_id BIGINT DEFAULT NULL;
ALTER TABLE work_order MODIFY COLUMN handler_id BIGINT DEFAULT NULL;
ALTER TABLE work_order MODIFY COLUMN requester_org_id BIGINT DEFAULT NULL;
ALTER TABLE work_order_item MODIFY COLUMN work_order_id BIGINT NOT NULL;
ALTER TABLE work_order_item MODIFY COLUMN target_id BIGINT DEFAULT NULL;

-- ===== 4. 重建外键约束 =====
ALTER TABLE area_code_org_mapping ADD CONSTRAINT fk_area_org FOREIGN KEY (org_id) REFERENCES org_structure(id);
ALTER TABLE cost_center_mapping ADD CONSTRAINT fk_cc_org FOREIGN KEY (org_id) REFERENCES org_structure(id);
ALTER TABLE employee ADD CONSTRAINT fk_employee_org FOREIGN KEY (org_id) REFERENCES org_structure(id);
ALTER TABLE extension_pool ADD CONSTRAINT fk_pool_org FOREIGN KEY (org_id) REFERENCES org_structure(id);
ALTER TABLE invoice ADD CONSTRAINT invoice_ibfk_1 FOREIGN KEY (source_org_id) REFERENCES org_structure(id);
ALTER TABLE invoice ADD CONSTRAINT invoice_ibfk_2 FOREIGN KEY (recipient_org_id) REFERENCES org_structure(id);
ALTER TABLE invoice_distribution ADD CONSTRAINT invoice_distribution_ibfk_1 FOREIGN KEY (invoice_id) REFERENCES invoice(id);
ALTER TABLE invoice_file ADD CONSTRAINT invoice_file_ibfk_1 FOREIGN KEY (invoice_id) REFERENCES invoice(id);
ALTER TABLE org_structure ADD CONSTRAINT fk_org_parent FOREIGN KEY (parent_id) REFERENCES org_structure(id);
ALTER TABLE phone_history ADD CONSTRAINT fk_history_phone FOREIGN KEY (phone_id) REFERENCES phone_number(id);
ALTER TABLE phone_number ADD CONSTRAINT fk_phone_alloc_org FOREIGN KEY (allocation_org_id) REFERENCES org_structure(id);
ALTER TABLE phone_number ADD CONSTRAINT fk_phone_org FOREIGN KEY (org_id) REFERENCES org_structure(id);
ALTER TABLE sys_user ADD CONSTRAINT fk_user_org FOREIGN KEY (scope_org_id) REFERENCES org_structure(id);
ALTER TABLE sys_user ADD CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id);
ALTER TABLE work_order_item ADD CONSTRAINT work_order_item_ibfk_1 FOREIGN KEY (work_order_id) REFERENCES work_order(id);
-- fk_user_employee 引用 employee_no (varchar)，非 bigint 主键，跳过重建
