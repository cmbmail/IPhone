-- 号码费用专用字段
ALTER TABLE bill_raw ADD COLUMN user_id VARCHAR(20) NULL COMMENT "用户ID" AFTER phone_number;
ALTER TABLE bill_raw ADD COLUMN department VARCHAR(100) NULL COMMENT "部门" AFTER user_id;
ALTER TABLE bill_raw ADD COLUMN allocation_time DATE NULL COMMENT "分配时间" AFTER department;
ALTER TABLE bill_raw ADD COLUMN platform_usage_fee DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT "平台使用费" AFTER allocation_time;
ALTER TABLE bill_raw ADD COLUMN number_monthly_rent DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT "码号月租费" AFTER platform_usage_fee;
ALTER TABLE bill_raw ADD COLUMN outbound_duration INT NULL DEFAULT 0 COMMENT "外呼时长(秒)" AFTER number_monthly_rent;
ALTER TABLE bill_raw ADD COLUMN transfer_outbound_duration INT NULL DEFAULT 0 COMMENT "转接外呼时长(秒)" AFTER outbound_duration;
ALTER TABLE bill_raw ADD COLUMN domestic_charge DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT "国内费用" AFTER transfer_outbound_duration;
ALTER TABLE bill_raw ADD COLUMN international_duration INT NULL DEFAULT 0 COMMENT "国际时长(秒)" AFTER domestic_charge;
ALTER TABLE bill_raw ADD COLUMN international_charge DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT "国际费用" AFTER international_duration;

-- 录音费用专用字段
ALTER TABLE bill_raw ADD COLUMN extension_number VARCHAR(20) NULL COMMENT "分机号" AFTER phone_number;
ALTER TABLE bill_raw ADD COLUMN activation_time DATE NULL COMMENT "开启时间" AFTER international_charge;
ALTER TABLE bill_raw ADD COLUMN deactivation_time DATE NULL COMMENT "关闭时间" AFTER activation_time;
ALTER TABLE bill_raw ADD COLUMN days INT NULL DEFAULT 0 COMMENT "天数" AFTER deactivation_time;

-- 通用备注
ALTER TABLE bill_raw ADD COLUMN remark VARCHAR(500) NULL COMMENT "备注" AFTER days;
