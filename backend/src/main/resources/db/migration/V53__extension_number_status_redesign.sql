-- V53: Redesign extension_number.status values
-- Old: 0=可分配 1=已占用 2=闲置(无电话)
-- New: 0=可用(无外线,显示闲置) 1=占用(有外线,无工单) 2=分配中(有外线,工单未完成)

-- Migrate: old status=2 (闲置/无电话) → status=0 (可用/闲置)
UPDATE extension_number SET status = 0 WHERE status = 2;

-- Update column comment
ALTER TABLE extension_number MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0:可用 1:占用 2:分配中';
