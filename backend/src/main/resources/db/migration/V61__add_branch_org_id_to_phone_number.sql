-- V61: Add branch_org_id column for two-phase phone allocation
ALTER TABLE phone_number ADD COLUMN branch_org_id BIGINT NULL AFTER org_id;

-- Add index for branch pool queries
CREATE INDEX idx_phone_branch_org_id ON phone_number(branch_org_id);

-- Add index for branch pool + no dept filter
CREATE INDEX idx_phone_branch_org_no_dept ON phone_number(branch_org_id, org_id);
