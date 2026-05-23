-- V50: Fix announcement.priority encoding to match work_order.priority convention
-- Current(announcement): 1=紧急 2=高 3=普通 4=低 (reversed)
-- Current(work_order):   1=低 2=普通 3=高 4=紧急 (standard)
-- After fix: unified     1=低 2=普通 3=高 4=紧急

-- Step 1: Swap values using a temporary value (0) to avoid conflicts
UPDATE announcement SET priority = 0 WHERE priority = 1;
UPDATE announcement SET priority = 1 WHERE priority = 4;
UPDATE announcement SET priority = 4 WHERE priority = 0;

UPDATE announcement SET priority = 0 WHERE priority = 2;
UPDATE announcement SET priority = 2 WHERE priority = 3;
UPDATE announcement SET priority = 3 WHERE priority = 0;

-- Step 2: Update column comment to match unified convention
ALTER TABLE announcement MODIFY COLUMN priority TINYINT NOT NULL DEFAULT 2 COMMENT '1:低 2:普通 3:高 4:紧急';
