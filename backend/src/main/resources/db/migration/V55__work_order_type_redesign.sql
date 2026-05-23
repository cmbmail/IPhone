-- V55: Redesign work_order.type to 5 business types
-- Old: 1:号码分配 2:号码转移 3:号码变更 4:组织变更 5:号码回收 6:号码交回 7:号码启用 8:号码停用
-- New: 1:新增 2:变更 3:解绑 4:座机绑定 5:号码拆机

-- Step 1: Migrate existing data
-- Old 1(号码分配)→1(新增): no change needed
-- Old 2(号码转移)→2(变更)
UPDATE work_order SET type = 2 WHERE type = 2;
-- Old 3(号码变更)→2(变更)
UPDATE work_order SET type = 2 WHERE type = 3;
-- Old 4(组织变更)→2(变更)
UPDATE work_order SET type = 2 WHERE type = 4;
-- Old 5(号码回收)→3(解绑)
UPDATE work_order SET type = 3 WHERE type = 5;
-- Old 6(号码交回)→3(解绑)
UPDATE work_order SET type = 3 WHERE type = 6;
-- Old 7(号码启用)→2(变更): 启用归入变更
UPDATE work_order SET type = 2 WHERE type = 7;
-- Old 8(号码停用)→5(号码拆机)
UPDATE work_order SET type = 5 WHERE type = 8;

-- Step 2: Migrate work_order_item.action (stores old type values as strings)
UPDATE work_order_item SET action = '1' WHERE action IN ('1');  -- 号码分配→新增 (no change)
UPDATE work_order_item SET action = '2' WHERE action IN ('2','3','4','7');  -- 转移/变更/组织变更/启用→变更
UPDATE work_order_item SET action = '3' WHERE action IN ('5','6');  -- 回收/交回→解绑
UPDATE work_order_item SET action = '5' WHERE action = '8';  -- 停用→号码拆机

-- Step 3: Update column comment
ALTER TABLE work_order MODIFY COLUMN type TINYINT NOT NULL DEFAULT 1 COMMENT '1:新增 2:变更 3:解绑 4:座机绑定 5:号码拆机';
