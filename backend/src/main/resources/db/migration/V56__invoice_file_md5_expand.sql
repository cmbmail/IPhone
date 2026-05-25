-- V56: Expand invoice_file.md5 column from VARCHAR(32) to VARCHAR(64) to accommodate SHA-256 hashes
ALTER TABLE invoice_file MODIFY COLUMN md5 VARCHAR(64);
