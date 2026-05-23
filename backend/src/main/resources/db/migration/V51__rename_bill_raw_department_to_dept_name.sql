-- V51: Rename bill_raw.department → dept_name for naming consistency
-- Other tables (extension_number, phone_ownership) already use dept_name

ALTER TABLE bill_raw CHANGE COLUMN department dept_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称';
