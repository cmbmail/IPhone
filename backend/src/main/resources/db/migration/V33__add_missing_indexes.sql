-- V33: Add missing columns and indexes for performance

-- Add missing columns first
ALTER TABLE bill_raw ADD COLUMN charge_type TINYINT NULL DEFAULT 0 COMMENT '费用类型: 0=号码 1=录音 2=彩铃 3=闪信' AFTER status;
ALTER TABLE notification ADD COLUMN user_id VARCHAR(100) NULL DEFAULT NULL COMMENT '用户标识' AFTER id;
ALTER TABLE bill_allocation ADD COLUMN org_id BIGINT NULL DEFAULT NULL COMMENT '组织ID' AFTER bill_month;

-- Create indexes (safe, skip if exists via pre-check)
CREATE INDEX idx_bill_raw_charge_type ON bill_raw(charge_type);
CREATE INDEX idx_bill_raw_bill_month ON bill_raw(bill_month);
CREATE INDEX idx_bill_allocation_month ON bill_allocation(bill_month);
CREATE INDEX idx_bill_allocation_org ON bill_allocation(org_id);
CREATE INDEX idx_work_order_handler ON work_order(handler_id);
CREATE INDEX idx_work_order_status ON work_order(status);
CREATE INDEX idx_phone_number_org ON phone_number(org_id);
CREATE INDEX idx_phone_number_status ON phone_number(status);
CREATE INDEX idx_employee_org_id ON employee(org_id);
CREATE INDEX idx_employee_status ON employee(status);
CREATE INDEX idx_invoice_bill_month ON invoice(bill_month);
CREATE INDEX idx_notification_user_status ON notification(user_id, status);
