-- V44: Iron Rule 3 - Fix total_amount precision from (14,2) to (12,2)
ALTER TABLE subsidiary_reconciliation
    MODIFY COLUMN total_amount DECIMAL(12,2) NOT NULL;
