-- V54: Add extension_number column to phone_device for MAC-to-extension association
-- "MAC通过分机号关联，同一个分机号有多个MAC"

ALTER TABLE phone_device ADD COLUMN extension_number VARCHAR(20) DEFAULT NULL COMMENT '关联分机号' AFTER assigned_employee_no;

-- Create index for fast lookup
CREATE INDEX idx_phone_device_extension_number ON phone_device(extension_number);
