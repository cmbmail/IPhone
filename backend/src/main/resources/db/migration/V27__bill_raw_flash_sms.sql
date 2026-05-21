-- V27: Add flash SMS and missing fields to bill_raw
ALTER TABLE bill_raw ADD COLUMN sub_number VARCHAR(20) DEFAULT NULL;
ALTER TABLE bill_raw ADD COLUMN city VARCHAR(50) DEFAULT NULL;
ALTER TABLE bill_raw ADD COLUMN send_count INT DEFAULT NULL;
ALTER TABLE bill_raw ADD COLUMN closing_time VARCHAR(50) DEFAULT NULL;
