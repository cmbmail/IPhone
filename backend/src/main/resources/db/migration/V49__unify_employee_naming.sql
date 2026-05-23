-- V49: Unify naming - employee_no for user identification, employee_name for user name
-- Concept: 使用人 = 电话号码的使用人，使用人有唯一的员工ID(employee_no)，员工ID为6位

-- 1. phone_number.user_id -> employee_no (was storing employee work number, misleading as user_id)
ALTER TABLE phone_number CHANGE COLUMN user_id employee_no VARCHAR(50) DEFAULT NULL COMMENT '使用人员工工号(6位)';

-- 2. bill_raw.user_id -> employee_no (same misleading name)
ALTER TABLE bill_raw CHANGE COLUMN user_id employee_no VARCHAR(50) DEFAULT NULL COMMENT '使用人员工工号(6位)';

-- 3. phone_history.from_user -> from_employee_no (standardize user -> employee_no)
ALTER TABLE phone_history CHANGE COLUMN from_user from_employee_no VARCHAR(50) DEFAULT NULL COMMENT '原使用人工号';

-- 4. phone_history.to_user -> to_employee_no
ALTER TABLE phone_history CHANGE COLUMN to_user to_employee_no VARCHAR(50) DEFAULT NULL COMMENT '新使用人工号';

-- 5. phone_surrender_record.final_user -> final_employee_no
ALTER TABLE phone_surrender_record CHANGE COLUMN final_user final_employee_no VARCHAR(50) DEFAULT NULL COMMENT '最后使用人工号';

-- 6. extension_number.user_name -> employee_name (unify with phone_snapshot.employee_name)
ALTER TABLE extension_number CHANGE COLUMN user_name employee_name VARCHAR(100) DEFAULT NULL COMMENT '使用人姓名';

-- 7. phone_snapshot.extension -> extension_number (abbreviation inconsistency)
ALTER TABLE phone_snapshot CHANGE COLUMN extension extension_number VARCHAR(20) DEFAULT NULL COMMENT '分机号';

-- 8. phone_device.assigned_to -> assigned_employee_no (clarify it stores employee number)
ALTER TABLE phone_device CHANGE COLUMN assigned_to assigned_employee_no VARCHAR(50) DEFAULT NULL COMMENT '使用人工号(6位)';

-- 9. phone_device_history.from_assigned -> from_assigned_employee_no
ALTER TABLE phone_device_history CHANGE COLUMN from_assigned from_assigned_employee_no VARCHAR(50) DEFAULT NULL COMMENT '原使用人工号';

-- 10. phone_device_history.to_assigned -> to_assigned_employee_no
ALTER TABLE phone_device_history CHANGE COLUMN to_assigned to_assigned_employee_no VARCHAR(50) DEFAULT NULL COMMENT '新使用人工号';
