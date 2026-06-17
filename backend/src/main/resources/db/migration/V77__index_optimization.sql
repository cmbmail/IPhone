-- ========================================================
-- V77: 索引优化 — 修复慢查询
-- 问题：phone_snapshot/bill_raw 缺少复合索引，导致大表扫描
-- 同时清理冗余索引、为 deleted_at 添加索引
-- ========================================================

-- ==================== 关键复合索引 ====================

-- 1. phone_snapshot: (snapshot_month, status) — 快照按状态过滤
CREATE INDEX idx_snap_month_status ON phone_snapshot(snapshot_month, status);

-- 2. phone_snapshot: (snapshot_month, branch_org_id) — 快照按分行过滤
CREATE INDEX idx_snap_month_branch ON phone_snapshot(snapshot_month, branch_org_id);

-- 3. phone_snapshot: (snapshot_month, org_id) — 快照按组织过滤
CREATE INDEX idx_snap_month_org ON phone_snapshot(snapshot_month, org_id);

-- 4. bill_raw: (bill_month, phone_number) — 费用分摊聚合核心查询
CREATE INDEX idx_bill_month_phone ON bill_raw(bill_month, phone_number);

-- 5. phone_ownership: (phone_number) 已有 uk_phone_number，补充 branch_org_id+phone_number 复合
--    用于分行筛选+分页
CREATE INDEX idx_ownership_branch_phone ON phone_ownership(branch_org_id, phone_number);

-- ==================== deleted_at 索引 (JPA @Where) ====================

-- 6. 大表 deleted_at 索引
CREATE INDEX idx_bill_raw_deleted ON bill_raw(deleted_at);
CREATE INDEX idx_phone_snapshot_deleted ON phone_snapshot(deleted_at);
CREATE INDEX idx_phone_ownership_deleted ON phone_ownership(deleted_at);
CREATE INDEX idx_phone_number_deleted ON phone_number(deleted_at);
CREATE INDEX idx_work_order_deleted ON work_order(deleted_at);
CREATE INDEX idx_employee_deleted ON employee(deleted_at);
CREATE INDEX idx_extension_number_deleted ON extension_number(deleted_at);
CREATE INDEX idx_org_structure_deleted ON org_structure(deleted_at);
CREATE INDEX idx_device_deleted ON device(deleted_at);
CREATE INDEX idx_extension_pool_deleted ON extension_pool(deleted_at);

-- ==================== 删除冗余索引 ====================

-- 7. bill_raw: idx_bill_raw_bill_month 与 idx_bill_month 完全重复
DROP INDEX idx_bill_raw_bill_month ON bill_raw;

-- 8. phone_number: idx_status 被 idx_phone_number_status 覆盖
DROP INDEX idx_status ON phone_number;

-- 9. employee: idx_employee_org_id 与 idx_org 完全重复
DROP INDEX idx_employee_org_id ON employee;

-- 10. phone_number: idx_org 与 idx_phone_number_org 重复
DROP INDEX idx_org ON phone_number;

-- 11. phone_number: idx_phone_branch_org_no_dept 被 idx_phone_branch_org_id 覆盖(低效)
DROP INDEX idx_phone_branch_org_no_dept ON phone_number;

-- 12. bill_raw: idx_bill_raw_charge_type 单列低基数索引(只有2个值)无效
DROP INDEX idx_bill_raw_charge_type ON bill_raw;

