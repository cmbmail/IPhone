-- V47: Rename employee.phone -> employee.phone_number for naming consistency
-- All other tables use 'phone_number' (varchar 50); employee used 'phone' (varchar 20)
ALTER TABLE employee CHANGE COLUMN phone phone_number VARCHAR(50) DEFAULT NULL COMMENT '联系电话';
