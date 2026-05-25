-- V59: Add created_by/updated_by audit columns to tables whose Entity extends BaseEntity but DB columns are missing
-- BaseEntity defines created_by and updated_by with default "system" in prePersist
ALTER TABLE subsidiary_reconciliation ADD COLUMN created_by VARCHAR(100) DEFAULT 'system', ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system';
ALTER TABLE phone_device_history ADD COLUMN created_by VARCHAR(100) DEFAULT 'system', ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system';
ALTER TABLE phone_snapshot ADD COLUMN created_by VARCHAR(100) DEFAULT 'system', ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system';
ALTER TABLE phone_surrender_record ADD COLUMN created_by VARCHAR(100) DEFAULT 'system', ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system';
ALTER TABLE phone_history ADD COLUMN created_by VARCHAR(100) DEFAULT 'system', ADD COLUMN updated_by VARCHAR(100) DEFAULT 'system';
