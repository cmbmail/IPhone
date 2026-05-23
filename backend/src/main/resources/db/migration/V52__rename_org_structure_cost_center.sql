-- V52: Rename org_structure.cost_center → cost_center_code
-- to match bill_allocation.cost_center_code, phone_snapshot.cost_center_code, cost_center_mapping.cost_center_code

ALTER TABLE org_structure CHANGE COLUMN cost_center cost_center_code VARCHAR(50) DEFAULT NULL COMMENT '成本中心编码';

-- V52 part 2: Drop orphan column import_batch.batch_no (Entity only uses batch_id)
ALTER TABLE import_batch DROP COLUMN batch_no;

-- V52 part 3: Drop orphan columns from device_operation
-- operation_data (json) is unmapped, Entity uses params (text) instead
-- result_message (varchar) is unmapped, Entity uses result (text) instead
ALTER TABLE device_operation DROP COLUMN operation_data;
ALTER TABLE device_operation DROP COLUMN result_message;
