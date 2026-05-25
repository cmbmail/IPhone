-- V57: Add missing audit columns to invoice_distribution table
ALTER TABLE invoice_distribution
  ADD COLUMN created_by VARCHAR(100) DEFAULT NULL AFTER created_at,
  ADD COLUMN updated_by VARCHAR(100) DEFAULT NULL AFTER updated_at;
