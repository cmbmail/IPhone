-- V75: phone_snapshot enhancements for bill association
-- 1. Add branch_org_id column for branch-level allocation
ALTER TABLE phone_snapshot ADD COLUMN branch_org_id BIGINT DEFAULT NULL AFTER org_id;

-- 2. Add bill_month link column (snapshot can be linked to a specific bill cycle)
ALTER TABLE phone_snapshot ADD COLUMN bill_month VARCHAR(7) DEFAULT NULL AFTER snapshot_month;

-- 3. Add allocation_status column: 0=未分摊 1=已分摊 2=分摊异常
ALTER TABLE phone_snapshot ADD COLUMN allocation_status TINYINT NOT NULL DEFAULT 0 AFTER is_allocatable;

-- 4. Add index for bill_month lookups
CREATE INDEX idx_snapshot_bill_month ON phone_snapshot(bill_month);
CREATE INDEX idx_snapshot_branch ON phone_snapshot(branch_org_id);

-- 5. Populate branch_org_id from org structure path (best-effort)
-- For existing data, derive branch from org_id's parent chain
