-- V33: Add missing indexes for performance
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
