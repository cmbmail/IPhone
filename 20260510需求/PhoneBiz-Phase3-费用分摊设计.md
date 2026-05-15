# PhoneBiz Phase 3 — 费用分摊模块设计

> 版本：1.0 | 日期：2026-05-10 | Phase 3 实现
> 来源：PRD §2.4 + 业务规则 BILL-001~005 + DDL

---

## 1. 核心流程

```
[Ops导入账单Excel] → [系统匹配当月快照] → [生成分摊明细]
→ [行政确认部门归属] → [行政确认金额] → [财务确认异常] → [财务提交锁定]
```

---

## 2. 功能清单（14 项）

| # | 功能 | 操作人 | 说明 |
|:--:|------|--------|------|
| 1 | Excel 账单导入 | Ops | 上传运营商账单 .xlsx；选择账单月份（YYYY-MM）；数据入 bill_raw 永久保留 |
| 2 | 账单列映射配置 | Ops | 配置 Excel 列与系统字段对应关系 |
| 3 | 自动分摊匹配 | 系统 | 根据当月 phone_snapshot 匹配号码归属部门和成本中心 |
| 4 | 成本中心对照维护 | 集团财务 | 部门 ↔ 成本中心代码映射（多对多） |
| 5 | 分摊明细预览 | Admin/财务 | 查看系统生成的分摊明细表 |
| 6 | 行政确认部门归属 | Admin | 逐条/批量确认 → 错误可驳回 |
| 7 | 行政确认费用金额 | Admin | 逐条/批量确认 → 错误可驳回 |
| 8 | 异常自动检测 | 系统 | 金额波动>±50%、无快照有账单、金额为空/异常大 |
| 9 | 财务确认异常 | 财务 | 审核异常项 → 确认/驳回 |
| 10 | 财务最终提交 | 财务 | 提交后数据锁定不可修改 |
| 11 | 驳回重新处理 | Ops | 任一步骤驳回 → Ops 重新处理 |
| 12 | 月度快照生成 | 系统 | 每月1日自动生成上月快照 |
| 13 | 子公司对账 | 子公司财务 | 查看分摊汇总 → 向集团财务确认 |
| 14 | 分摊报表 | 全员 | 部门/月度费用汇总；历史对比；异常清单 |

---

## 3. 分摊确认流水线（6 步）

| 步骤 | 操作人 | 内容 | 操作字段 | 可驳回 |
|:--:|--------|------|----------|:--:|
| 1 | Ops | 导入账单 | `import_status = processed` | — |
| 2 | 系统 | 自动匹配快照 | 写入 bill_allocation | — |
| 3 | Admin | 确认部门归属 | `admin_confirm_org = correct/wrong` | ✅ |
| 4 | Admin | 确认金额 | `admin_confirm_amount = correct/wrong` | ✅ |
| 5 | 财务 | 确认异常 | `finance_confirm_anomaly = confirmed/rejected` | ✅ |
| 6 | 财务 | 最终提交 | `finance_confirm_submit = submitted` | — |

---

## 4. 数据表（5 张）

| 表 | 关键字段 |
|----|---------|
| **bill_raw** | bill_month / phone_number / charge_amount / charge_type / raw_data(JSON) / import_status |
| **bill_allocation** | bill_month / phone_number / snapshot_org_id / cost_center_code / charge_amount / anomaly_flag / 6步确认状态字段 |
| **phone_snapshot** | snapshot_month / phone_id / org_id / cost_center_code / is_allocatable / is_surrendered |
| **cost_center_mapping** | org_id / cost_center_name / cost_center_code |
| **subsidiary_reconciliation** | bill_month / subsidiary_org_id / total_amount / invoice_count / reconciliation_status |

---

## 5. 异常检测规则

| 条件 | 自动标记 |
|------|---------|
| 金额与历史月均差异 > ±50% | `anomaly_flag=1` |
| 号码当月无快照但有账单 | `anomaly_flag=1` |
| 金额为 0 或异常大（>均值的 5σ） | `anomaly_flag=1` |

---

## 6. 快照生成规则

- **纳入分摊**：active + stopped（is_allocatable=1）
- **不纳入**：idle（is_allocatable=0）
- **纳入但标记**：cancelled（is_allocatable=0, is_surrendered=1）
- **生成时机**：每月1日自动 + 支持手动触发
- **数据锁定**：生成后不可修改

---

## 7. Phase 1 预留

| 预留项 | Phase 1 状态 |
|--------|:--:|
| 5 张表 DDL | ✅ 已在 DDL 中定义 |
| 功能开关 | `sys_feature_flag.billing=0` |
| Excel 导入框架 | ✅ M4 Phase 1 已实现（可复用） |
| 异步+轮询模式 | ✅ M4 Phase 1 已实现（可复用） |

---

*Phase 3 实现时，复用 Phase 1 的 Excel 异步导入 + 轮询 + batchInsert 框架，追加 6 步确认流水线。*
