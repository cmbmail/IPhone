# PhoneBiz Phase 1 — M10：电话机管理（新增模块）

> 新增日期：2026-05-10 | 基于 Q26-Q30 确认
> 状态：已确认，待纳入需求清单

---

## 1. 核心概念

电话机（Phone Device）是企业内部的话机硬件资产管理。话机通过**分机号**与电话号码关联（M:N）。

### 关键规则

- **M:N 关系**：一人可以用多台话机 / 一台话机可以多人使用
- **绑定维度**：话机与**分机号**绑定（非直接绑定号码）。device_phone_mapping 存储 phone_id，但 UI/API 以 extension_number 为绑定标识
- **绑定前置**：只有持有分机号（extension_number 非空）的号码才能被绑定到话机
- **设备归属**：设备属于组织（org_id），同时可分配给特定员工使用（assigned_to）
- **多线路**：一台话机可以绑定多个分机号（如多线路话机）
- **MAC 归一化**：输入接受 `A4:B1:C2:D3:E4:F5` 或 `A4B1C2D3E4F5`，统一存储为**大写12位十六进制**（去冒号）
- **离职联动**：员工离职时，自动回收所有分配的话机（assigned_to=NULL，状态→stock）
- **权限**：Admin 只看 scope 内组织的话机；Ops 全局

---

## 2. 数据模型

### 2.1 电话机主表

```sql
CREATE TABLE phone_device (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    mac_address     VARCHAR(12) NOT NULL COMMENT 'MAC地址（大写12位十六进制，去冒号存储）',
    model           VARCHAR(100) NULL COMMENT '型号',
    brand           VARCHAR(100) NULL COMMENT '品牌',
    purchase_date   DATE NULL COMMENT '购置日期',
    org_id          BIGINT UNSIGNED NOT NULL COMMENT '归属组织',
    assigned_to     VARCHAR(20) NULL COMMENT '分配使用人工号（可为空，表示组织公共设备）',
    status          ENUM('stock','active','inactive','repairing','retired') NOT NULL DEFAULT 'stock',
    remark          VARCHAR(500) NULL COMMENT '备注',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_by      VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_mac (mac_address),
    INDEX idx_org (org_id),
    INDEX idx_assigned (assigned_to),
    INDEX idx_status (status),
    FOREIGN KEY (org_id) REFERENCES org_structure(id)
) COMMENT='电话机主表';
```

### 2.2 话机-号码关联表（M:N）

```sql
CREATE TABLE device_phone_mapping (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT UNSIGNED NOT NULL COMMENT 'phone_device.id',
    phone_id    BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id（对应的分机号载体）',
    line_order  INT NOT NULL DEFAULT 1 COMMENT '线路序号',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_phone (device_id, phone_id),
    INDEX idx_phone (phone_id),
    FOREIGN KEY (device_id) REFERENCES phone_device(id),
    FOREIGN KEY (phone_id) REFERENCES phone_number(id)
) COMMENT='话机-号码关联表（通过分机号绑定，phone_id对应的号码必须持有分机号）';
```

### 2.3 话机操作历史

```sql
CREATE TABLE phone_device_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       BIGINT UNSIGNED NOT NULL,
    mac_address     VARCHAR(12) NOT NULL COMMENT '操作时MAC快照',
    action          VARCHAR(30) NOT NULL COMMENT '分配/回收/维修/报废/恢复',
    from_status     VARCHAR(20) NULL,
    to_status       VARCHAR(20) NULL,
    from_assigned   VARCHAR(20) NULL,
    to_assigned     VARCHAR(20) NULL,
    operator        VARCHAR(50) NOT NULL,
    operated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500) NULL,
    INDEX idx_device (device_id),
    INDEX idx_operated_at (operated_at),
    FOREIGN KEY (device_id) REFERENCES phone_device(id)
) COMMENT='话机操作历史';
```

---

## 3. 状态机

```
┌──────────┐  分配    ┌──────────┐  送修    ┌────────────┐
│  STOCK   │────────→│  ACTIVE  │────────→│ REPAIRING  │
│  库存    │←────────│  在用    │←────────│  维修中     │
└────┬─────┘  回收   └────┬─────┘  修复   └──────┬─────┘
     │停用                │停用                   │报废
     ▼                    ▼                       ▼
┌──────────┐←────────────────────────────┐
│ INACTIVE │                             │
│  停用    │─────────────────────────────→│
└────┬─────┘  恢复                        │
     │                                    ▼
     │报废                        ┌──────────┐
     └───────────────────────────→│ RETIRED  │
                                  │  报废    │
                                  └──────────┘
```

| 状态 | 含义 | 设置者 | 可转入来源 | 可转出目标 |
|------|------|--------|-----------|-----------|
| **stock** | 库存/未分配 | Ops | —（初始）/ inactive 停用 | active（分配） |
| **active** | 正常使用中 | Admin | stock / repairing | inactive / repairing / retired |
| **inactive** | 停用 | Admin | active / stock | stock / retired |
| **repairing** | 维修中 | Admin/Ops | active | active（修复完成）/ retired（无法修复） |
| **retired** | 报废 | Admin/Ops | active / inactive / repairing / stock | —（终态） |

---

## 4. 操作

| ID | 操作 | 状态转移 | P | 说明 |
|----|------|----------|:--:|------|
| DEV-01 | 录入话机 | — → stock | P0 | Ops 录入 MAC+型号+品牌+购置日期+归属组织 |
| DEV-02 | 分配话机 | stock → active | P0 | 分配给员工（assigned_to=employeeNo）；Admin scope |
| DEV-03 | 回收话机 | active → stock | P1 | 解除员工绑定（assigned_to=NULL） |
| DEV-04 | 停用 | active/stock → inactive | P1 | 暂时停止使用 |
| DEV-05 | 恢复 | inactive → stock | P1 | 恢复为库存 |
| DEV-06 | 送修 | active → repairing | P1 | 登记送修；**自动解绑全部号码** |
| DEV-07 | 修复 | repairing → active | P1 | 维修完成恢复使用 |
| DEV-08 | 报废 | any → retired | P0 | 终态，不可逆 |
| DEV-09 | 绑定号码 | — | P1 | 按**分机号**查询并选择号码 → 创建 device_phone_mapping；目标号码必须有分机号 |
| DEV-10 | 解绑号码 | — | P1 | 删除 device_phone_mapping 记录 |

### 操作约束

- 报废前：自动解绑所有关联号码
- 分配时：校验目标员工是否存在且 active
- 绑定号码时：校验目标号码 `extension_number IS NOT NULL`（**无分机号的号码不可绑定**）
- MAC 归一化：输入时去冒号+转大写+校验12位十六进制
- 同一 设备-号码 组合不可重复绑定
- 员工离职 → 号码和话机**各自独立事务**回收（无顺序依赖）
- 权限：话机列表按 Admin scope 裁剪（org_id 在 scope 内）；Ops 查看全局

### 联动规则

| 场景 | 话机行为 |
|------|---------|
| 号码 surrender（拆机） | **不自动解绑**；话机仍显示该号码（status=cancelled），作为历史参考 |
| 号码 reclaim（回收，分机号清空） | **不自动解绑**（绑定的是 phone_id）；话机显示该号码但分机号为空 |
| 员工离职 | 号码回收 + 话机回收 **各自独立执行**（任一失败不影响另一） |
| 话机送修（active→repairing） | **自动解绑全部号码**；修复后需重新绑定 |
| 话机报废（→retired） | **自动解绑全部号码** |
| 跨员工/跨组织绑定 | **允许**，话机和号码的 assigned_to/user_id 各自独立 |

---

## 5. API 端点（新增 12 个）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/devices | 话机列表（分页+状态过滤+组织过滤） | Admin scope + Ops |
| GET | /api/devices/{id} | 话机详情（含绑定号码+操作历史） | Admin + Ops |
| POST | /api/devices | 录入话机（MAC+型号+品牌+购置日期+组织） | Ops |
| PUT | /api/devices/{id} | 编辑话机信息 | Ops |
| POST | /api/devices/{id}/assign | 分配（stock→active） | Admin scope |
| POST | /api/devices/{id}/reclaim | 回收（active→stock） | Admin scope |
| POST | /api/devices/{id}/repair | 送修（active→repairing） | Admin/Ops |
| POST | /api/devices/{id}/repair-done | 修复（repairing→active） | Admin/Ops |
| POST | /api/devices/{id}/deactivate | 停用 | Admin |
| POST | /api/devices/{id}/reactivate | 恢复 | Admin |
| POST | /api/devices/{id}/retire | 报废（终态） | Admin/Ops |
| POST | /api/devices/{id}/bind-phone | 绑定号码（按分机号查询：`{ extensionNumber: "1234" }`） | Admin/Ops |
| DELETE | /api/devices/{id}/unbind-phone/{phoneId} | 解绑号码 | Admin/Ops |
| GET | /api/devices/{id}/history | 操作历史 | Admin + Ops |
| GET | /api/devices/{id}/phones | 已绑定号码列表（显示分机号+号码+状态） | Admin + Ops |

---

## 6. 新增数据校验

| 字段 | 必填 | 约束 |
|------|:--:|------|
| `mac_address` | ✅ | 归一化后为12位大写十六进制（0-9,A-F）；全局唯一 |
| `model` | ❌ | ≤100字符 |
| `brand` | ❌ | ≤100字符 |
| `purchase_date` | ❌ | 日期格式，不可为未来 |
| `org_id` | ✅ | 必须为有效组织 |

---

## 7. 新增错误码

| errorCode | HTTP | 含义 |
|-----------|------|------|
| `DEVICE_MAC_INVALID` | 400 | MAC 格式无效（非12位十六进制） |
| `DEVICE_MAC_DUPLICATE` | 400 | MAC 地址已存在 |
| `DEVICE_NOT_STOCK` | 400 | 话机非库存状态，无法分配 |
| `DEVICE_HAS_BOUND_PHONES` | 400 | 话机仍有绑定号码，请先解绑 |
| `DEVICE_PHONE_ALREADY_BOUND` | 400 | 号码已绑定到该话机 |
| `DEVICE_PHONE_NO_EXTENSION` | 400 | 目标号码无分机号，无法绑定 |

---

## 8. 按钮可见性

| 状态 | 可见按钮 |
|------|---------|
| stock | 分配、停用、报废 |
| active | 回收、停用、送修、报废、绑定号码 |
| inactive | 恢复、报废 |
| repairing | 修复、报废 |
| retired | 仅查看+历史 |

---

## 9. 增量变更汇总

| 变更项 | 内容 |
|--------|------|
| 新增表 | `phone_device`、`device_phone_mapping`、`phone_device_history` |
| 新增需求ID | DEV-01~DEV-10（10个） |
| 新增 API | 16 个端点 |
| 新增错误码 | 5 个 |
| 新增模块 | M10：电话机管理 |
| 新增任务 | 预估 12-15 项（实体+CRUD+操作+前端页面+绑定管理） |
