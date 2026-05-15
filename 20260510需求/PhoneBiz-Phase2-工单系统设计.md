# PhoneBiz Phase 2 — 工单系统设计

> 版本：1.0 | 日期：2026-05-10 | Phase 1 预留，Phase 2 实现
> 工单项含：姓名+工号+分机+号码+MAC+申请类型

---

## 1. 工单主表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `work_order_no` | VARCHAR(50) | 工单编号，全局唯一（如 WO-20260510-001） |
| `type` | VARCHAR(30) | 工单类型（13种） |
| `status` | ENUM | 6状态 |
| `priority` | ENUM | low / normal / high / urgent |
| `description` | VARCHAR(1000) | 工单原因/描述 |
| `requester` | VARCHAR(50) | 申请人（admin工号） |
| `requester_org_id` | BIGINT | 申请人部门 |
| `handler` | VARCHAR(50) | 处理人（ops工号） |
| `batch_id` | VARCHAR(50) | 批量工单批次号 |
| `accepted_at` / `completed_at` / `archived_at` | DATETIME | 时间节点 |

## 2. 工单项（核心）

| 字段 | 类型 | 说明 |
|------|------|------|
| **employee_no** | VARCHAR(20) | 🆕 工号 |
| **employee_name** | VARCHAR(50) | 🆕 姓名快照 |
| **extension_number** | VARCHAR(10) | 🆕 分机号 |
| `phone_number` | VARCHAR(20) | 电话号码 |
| **mac_address** | VARCHAR(12) | 🆕 MAC地址 |
| `action` | VARCHAR(30) | 申请类型 |
| `phone_id` / `device_id` | BIGINT | 关联FK |
| `from_user` / `to_user` | VARCHAR(20) | 变更前后使用人 |
| `from_org` / `to_org` | VARCHAR(200) | 变更前后组织 |
| `new_phone_number` / `new_org_id` | — | 换号/转移专用 |
| `remark` | VARCHAR(500) | 备注 |

## 3. 申请类型（13种）

```
号码类（7种）                    话机类（6种）
├── allocate      分配号码       ├── device_assign   分配话机
├── change_user   过户           ├── device_reclaim  回收话机
├── change_number 换号           ├── device_repair   送修
├── change_org    转移           ├── device_retire   报废
├── reclaim       回收           ├── device_bind     绑定号码
├── surrender     拆机           └── device_unbind   解绑号码
└── trouble       停复机
```

## 4. 填值规则

| action | phone字段 | device字段 | employee字段 | to/from |
|--------|:--:|:--:|:--:|:--:|
| allocate | ✅ | — | to_user | to |
| reclaim | ✅ | — | from_user | from |
| trouble | ✅ | — | — | — |
| surrender | ✅ | — | — | — |
| change-user | ✅ | — | from+to | both |
| change-number | ✅ | — | from+to | both |
| change-org | ✅ | — | — | — |
| device_assign | — | ✅ | to_user | to |
| device_reclaim | — | ✅ | from_user | from |
| device_repair | — | ✅ | — | — |
| device_retire | — | ✅ | — | — |
| device_bind | ✅ | ✅ | — | — |
| device_unbind | ✅ | ✅ | — | — |

## 5. 状态流转

```
pending → accepted → processing → completed → archived
  │                                        ↑
  └──────────── cancelled ─────────────────┘
```

- 无审批节点
- 无超时规则
- 仅系统消息推送
- 批量工单按 batch_id 关联 + 自动拆单

## 6. 示例

```
工单 WO-2026-0510-001 | 类型 device_assign | 状态 pending
申请人 admin01 | 行政部

项 #1 | action: device_assign
  姓名: 张三
  工号: 001234
  分机: 1234
  号码: 010-88880001
  MAC:  A4B1C2D3E4F5
  型号: Cisco 8841
  备注: 新员工入职分配话机
```

---

*Phase 1 不实现工单，仅保留 DDL。Phase 2 启用 sys_feature_flag.work_order=1 后开发。*
