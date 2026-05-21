-- V30: Add branch_name, org_code, cost_center to org_structure
ALTER TABLE org_structure ADD COLUMN branch_name VARCHAR(100) DEFAULT NULL;
ALTER TABLE org_structure ADD COLUMN org_code VARCHAR(50) DEFAULT NULL;
ALTER TABLE org_structure ADD COLUMN cost_center VARCHAR(50) DEFAULT NULL;

-- Add indexes for cost center lookup
CREATE INDEX idx_org_cost_center ON org_structure(cost_center);
CREATE INDEX idx_org_branch_name ON org_structure(branch_name);
