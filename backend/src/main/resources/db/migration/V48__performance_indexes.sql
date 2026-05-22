-- V48: Performance optimization - add composite indexes
-- These indexes are idempotent (IF NOT EXISTS) for safe re-execution

-- Composite indexes for common query patterns
-- 1. phone_number: queries by org_id + status
CREATE INDEX IF NOT EXISTS idx_phone_org_status ON phone_number (org_id, status);

-- 2. bill_allocation: queries by bill_month + snapshot_org_id
CREATE INDEX IF NOT EXISTS idx_bill_alloc_month_org ON bill_allocation (bill_month, snapshot_org_id);

-- 3. bill_allocation: queries by bill_month + anomaly_flag
CREATE INDEX IF NOT EXISTS idx_bill_alloc_month_anomaly ON bill_allocation (bill_month, anomaly_flag);

-- 4. work_order: queries by status + created_at
CREATE INDEX IF NOT EXISTS idx_wo_status_created ON work_order (status, created_at);

-- 5. notification: queries by recipient + read status
CREATE INDEX IF NOT EXISTS idx_notif_recipient_read ON notification (recipient, read_at);

-- 6. employee: queries by org_id + status
CREATE INDEX IF NOT EXISTS idx_emp_org_status ON employee (org_id, status);
