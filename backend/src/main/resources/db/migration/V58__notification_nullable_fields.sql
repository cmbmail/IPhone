-- V58: Make notification.recipient and created_by nullable since the existing code doesn't populate them consistently
ALTER TABLE notification
  MODIFY COLUMN recipient VARCHAR(50) DEFAULT NULL,
  MODIFY COLUMN created_by VARCHAR(50) DEFAULT NULL,
  MODIFY COLUMN updated_by VARCHAR(50) DEFAULT NULL;
