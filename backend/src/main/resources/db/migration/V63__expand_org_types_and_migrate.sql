-- V63: Expand org_structure type system from 3 to 5 types
-- Type mapping:
--   1 = 集团 (ORG_GROUP)        -- unchanged
--   2 = 分行 (ORG_BRANCH)       -- was 子公司, now covers 一级分行/二级分行/总行
--   3 = 部门 (ORG_DEPT)         -- unchanged, covers 一级部门/二级部门
--   4 = 综合支行 (ORG_COMP_SUB)  -- NEW: comprehensive sub-branch
--   5 = 零专支行 (ORG_RETL_SUB)  -- NEW: retail/specialized sub-branch (under 综合支行)

-- Update column comment to reflect new type values
ALTER TABLE org_structure MODIFY COLUMN type TINYINT NOT NULL COMMENT '1=集团 2=分行 3=部门 4=综合支行 5=零专支行';

-- Move 监察室(id=18) from 集团(id=1) to 总行(id=12)
-- It currently: type=3, level=1, parent_id=1, path=/1/18
UPDATE org_structure SET
    parent_id = 12,
    level = 2,
    path = '/1/12/18'
WHERE id = 18;

-- Recalculate level for all orgs based on path depth (number of '/' segments - 1)
-- This ensures consistency after the move
UPDATE org_structure SET level = LENGTH(path) - LENGTH(REPLACE(path, '/', '')) - 1
WHERE deleted_at IS NULL;

-- Insert test 综合支行 under 一级分行s (type=4)
INSERT INTO org_structure (name, type, level, parent_id, path, sort_order, status, created_at, updated_at, created_by, updated_by) VALUES
('朝阳支行', 4, 2, 2, '/1/2/26', 1, 1, NOW(), NOW(), 'system', 'system'),
('中关村支行', 4, 2, 2, '/1/2/27', 2, 1, NOW(), NOW(), 'system', 'system'),
('陆家嘴支行', 4, 2, 3, '/1/3/28', 1, 1, NOW(), NOW(), 'system', 'system'),
('福田支行', 4, 2, 4, '/1/4/29', 1, 1, NOW(), NOW(), 'system', 'system');

-- Insert test 零专支行 under 综合支行 (type=5)
INSERT INTO org_structure (name, type, level, parent_id, path, sort_order, status, created_at, updated_at, created_by, updated_by) VALUES
('朝阳零售支行', 5, 3, 26, '/1/2/26/30', 1, 1, NOW(), NOW(), 'system', 'system'),
('朝阳信贷支行', 5, 3, 26, '/1/2/26/31', 2, 1, NOW(), NOW(), 'system', 'system'),
('中关村零售支行', 5, 3, 27, '/1/2/27/32', 1, 1, NOW(), NOW(), 'system', 'system');

-- Insert test 二级分行下的综合支行 (惠州分行=16, level=2)
INSERT INTO org_structure (name, type, level, parent_id, path, sort_order, status, created_at, updated_at, created_by, updated_by) VALUES
('陈江支行', 4, 3, 16, '/1/4/16/33', 1, 1, NOW(), NOW(), 'system', 'system');
