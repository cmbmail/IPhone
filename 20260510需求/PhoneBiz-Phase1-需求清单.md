# PhoneBiz Phase 1 需求清单

> 版本：5.2（M10 整合版） | 日期：2026-05-10 | 三轮 35 + 3 项 QA 确认
> 用途：Phase 1 开发实现的**唯一需求依据**

---

## 一、项目概述

企业内线电话业务管理平台（PhoneBiz）。Phase 1 交付：

- **6 状态号码生命周期**：idle / reserved / disabled / active / stopped / cancelled
- **分机号双轨**：auto（跟人）↔ manual（跟号）
- **组织架构**：多层级树 + 全局展示 + scope 置灰只读
- **员工管理**：CRUD + 工号规则 + 离职自动回收号码
- **号码导入**：Excel 批量导入 + 区号匹配 + 逐条录入 + 二次入库
- **权限**：RBAC（Admin/Ops/Finance/Boss）+ JWT + 防并发锁
- **电话机管理**：5 状态生命周期 + M:N 话机-分机号绑定 + MAC 归一化

**技术栈**：React 18 + Vite + Antd 5 / Spring Boot 3.2 + JPA + MySQL 8 / JWT

---

## 二、核心概念

### 2.1 号码状态机（6 状态）

```
                        ┌──────────────┐
                  ┌────→│  RESERVED    │←────┐
                  │     │  预留(Admin)  │     │
                  │解除  └──────┬───────┘     │预留
                  │            │surrender     │
                  ▼            ▼              │
┌──────────┐ allocate ┌──────────┐ reclaim ┌─┴─────────┐
│          │←─────────│          │←────────│            │
│  IDLE    │          │  ACTIVE  │         │  DISABLED  │
│  空闲    │          │  在用    │         │  禁用(封存) │
│          │          │          │         │            │
└────┬─────┘          └────┬─────┘         └─────┬──────┘
     │                     │  trouble            │
     │surrender       ┌────▼─────┐         disable│  ↑enable
     │                │  STOPPED │               │  │
     │                │  停机    │               │  │
     │                └────┬─────┘               │  │
     │                     │surrender            │  │
     ▼                     ▼                     ▼  │
┌──────────────────────────────────────────────────────┐
│                   CANCELLED                           │
│               已拆机（不可逆）                          │
│             可二次入库（重新录入）                       │
└──────────────────────────────────────────────────────┘
```

| 状态 | 含义 | 设置者 | 可转入来源 | 可转出目标 |
|------|------|--------|-----------|-----------|
| **idle** | 空闲可分配 | — | —（初始）/ reclaim / 解除预留 / 解除禁用 | reserved / disabled / active / cancelled |
| **reserved** | 预留（特殊用途保留） | Admin | idle | idle（解除预留）/ cancelled |
| **disabled** | 禁用（封存冻结） | Admin | idle | idle（解除禁用）/ cancelled |
| **active** | 正常使用中 | — | idle（allocate） | stopped / idle（reclaim）/ cancelled |
| **stopped** | 停机（欠费/申请暂停） | Admin/Ops | active | active（复机）/ idle（reclaim）/ cancelled |
| **cancelled** | 已拆机（不可逆） | Admin/Ops | idle/reserved/disabled/active/stopped | —（仅二次入库） |

### 2.2 分机号双轨

```
分机号
├── auto（跟人，自动分机号）
│   ├── 条件：员工工号为纯数字
│   ├── 生成：工号去前导0（001234 → 1234），实时计算
│   ├── 唯一性：仅校验当前使用中（不含已拆机）
│   └── 回收：extension_number 清空；员工保留分机号资格，下次复用
│
└── manual（跟号，人工分机号）
    ├── 条件：工号含字母 → 从号池分配；人工指定覆盖；公共号码
    ├── 来源：该部门分机号池随机选 或 手动指定
    ├── 归属：记录 allocation_org_id（原始分配部门）
    ├── 唯一性：全局唯一（含已拆机）
    └── 回收：extension_number 清空；归还到 allocation_org_id 的号池
```

> **分机号类型判定**：alloc/change-user 时始终按**目标员工**工号重新判定（纯数字→auto / 含字母→manual）。

### 2.3 号码来源

| 来源 | 方式 | 操作人 | 说明 |
|------|------|--------|------|
| 逐条录入 | PH-03 | Ops | 手动输入单个；归一化仅在导入时，逐条不归一化 |
| Excel 导入 | PH-05 | Ops | ≤500行；列：号码+一级子公司+二级子公司+备注 |
| 二次入库 | 自动检测 | 系统 | 录入时检测 surrender_record → 标记 is_reentry=1 |

### 2.4 公共号码

会议室/前台等公用号码 → 创建**虚拟员工**（`VIR-{部门缩写}-{序号}`，如 `VIR-FRONT-001`），人工分配分机号。

---

## 三、新增/修改的数据结构

### 3.1 phone_number 表变更

```sql
-- 新增字段
ALTER TABLE phone_number ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁';
ALTER TABLE phone_number ADD COLUMN extension_type ENUM('auto','manual') NULL COMMENT '分机号类型';
ALTER TABLE phone_number ADD COLUMN is_reentry TINYINT(1) NOT NULL DEFAULT 0 COMMENT '二次入库';
ALTER TABLE phone_number ADD COLUMN import_batch_id VARCHAR(50) NULL COMMENT '导入批次';
ALTER TABLE phone_number ADD COLUMN allocation_org_id BIGINT UNSIGNED NULL COMMENT 'manual分机号初始分配部门';

-- 修改 status 枚举（DDL 需重建或 ALTER）
-- status: ENUM('idle','reserved','disabled','active','stopped','cancelled')
```

### 3.2 区号-组织对照表（新表）

```sql
CREATE TABLE area_code_org_mapping (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    area_code       VARCHAR(10) NOT NULL COMMENT '区号（010/021/0755）',
    org_id          BIGINT UNSIGNED NOT NULL COMMENT '匹配组织',
    priority        INT NOT NULL DEFAULT 1 COMMENT '优先级（1=首选）',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES org_structure(id),
    UNIQUE KEY uk_area_org (area_code, org_id)
) COMMENT='区号-组织对照表';
```

### 3.3 系统预置数据

| 预置项 | 说明 |
|--------|------|
| 「待分配」虚拟组织 | org_id 固定（如 -1 或 0），初始化创建，不可删除 |
| 种子用户 | admin / ops / boss，默认密码 `Phonebiz@2026`，首次强制改密 |

---

## 四、数据约束

### 4.1 字段校验

| 字段 | 必填 | 约束 |
|------|:--:|------|
| `employee_no` | ✅ | 6位字母或数字；禁止全0；全局唯一 |
| `employee.name` | ✅ | 2-20字符；不含特殊符号 |
| `employee.phone` | ❌ | 手机1[3-9]\d{9} 或 固话0\d{2,3}-\d{7,8} |
| `employee.email` | ❌ | RFC 5322；如有则全局唯一 |
| `employee.is_virtual` | — | TINYINT(1) DEFAULT 0；虚拟员工=1 |
| `org.name` | ✅ | 2-50字符；同父节点下唯一 |
| `phone_number` | ✅ | 固话/手机号；全局唯一（含已拆机） |
| `extension_number` (manual) | ❌ | 6位纯数字；不以0开头；全局唯一（含已拆机） |
| `extension_number` (auto) | — | 实时计算，仅校验当前使用中（**不含**已拆机） |
| `username` | ✅ | 5-20位；字母开头；字母+数字+下划线；全局唯一 |
| `password` | ✅ | BCrypt；明文≥8位+大小写+数字+特殊字符≥3类 |
| `pool.start/end` | ✅ | 6位纯数字；start < end；同部门不重叠 |

### 4.2 边界值默认处理

| 场景 | 处理 |
|------|------|
| 工号不足/超过6位 | 拒绝，提示"工号必须为6位" |
| 号码格式（有/无横杠） | Excel 导入时归一化去空格横杠后判重；逐条录入不归一化 |
| 分页 page≤0 / size≤0 | 默认 page=1, size=20 |
| 号池 start=end | 拒绝 |
| Excel 同批次内重复号码 | 仅保留首条，后续标记"批次内重复" |
| org path 超 VARCHAR(500) | 拒绝创建，提示"组织层级过深" |
| 自动分机号不足6位 | 接受（如 000001→1），保持与工号直接映射 |

---

## 五、操作校验链

### 5.1 allocate（分配）— 跨组织允许

```
1. 号码 status = idle
2. 目标员工存在且 status = active
3. 目标员工无 active/stopped 号码
4. 号码 org_id 在操作人 scope 内；员工 org_id**不限制**（允许跨组织分配）
5. 分机号处理（按目标员工工号类型）：
   ├── 纯数字 → auto = 工号去前导0（校验当前使用中无冲突）
   ├── 含字母 → manual：从该部门号池随机选6位数字
   │   └── 号池已配置→随机选；无号池→拒绝 `EXT_POOL_REQUIRED`
   │   └── 选号最多重试100次→仍无可用→`EXT_POOL_EXHAUSTED`
   └── 人工指定 → manual：校验全局唯一+在目标部门号池内
6. extension_type='auto'|'manual'；manual 时 allocation_org_id=员工部门
7. SELECT ... FOR UPDATE 锁号码行 + 员工行
```

### 5.2 reclaim（回收）

```
1. 号码 status IN (active, stopped)
2. 号码 org_id 在操作人 scope 内
3. 分机号处理：
   ├── extension_type='auto' → extension_number 清空（员工保留资格）
   └── extension_type='manual' → extension_number 清空（归还 allocation_org_id 号池）
4. user_id 置空；extension_type 置空；allocation_org_id 置空
```

### 5.3 change-user（过户）

```
1. 号码 status = active
2. 新员工存在且 status = active
3. 新员工无 active/stopped 号码
4. 号码 org_id 在操作人 scope 内（不限制新员工 org）
5. 分机号**按新员工工号重新判定**（纯数字→auto / 含字母→manual）
   ├── 纯数字 → auto = 新员工工号去前导0
   ├── 含字母 → manual：从新员工部门号池分配
   ├── 双方都不符合 → 申请人手动决定
   └── 允许在操作过程中修改分机号
6. 原使用人失去号码；原 auto extension 保留资格；原 manual extension 归还原 allocation_org_id
```

### 5.4 change-number（换号）

```
1. 当前号码 status = active
2. 新号码 status = idle
3. 两号码 org_id 均在 scope 内
4. 新号码 != 当前号码
5. SELECT ... FOR UPDATE 锁**两个号码**
6. 事务：旧号码→idle（清空分机号）+ 新号码→active（继承员工分机号，重新判定类型）
7. 两条 phone_history
```

### 5.5 预留/禁用

```
预留：
  设置：idle → reserved | Admin(scope) | 必须填原因
  解除：reserved → idle | Admin(scope)
  
禁用：
  设置：idle → disabled | Admin(scope) | 必须填原因
  解除：disabled → idle | Admin(scope)
```

### 5.6 离职自动回收（EMP-04联动）

```
员工标记 inactive → 事务内自动 reclaim 其所有 active/stopped 号码
```

---

## 六、模块需求

### M1：认证与鉴权

| ID | 描述 | P | 验收标准 |
|----|------|:--:|----------|
| AUTH-01 | 登录 | P0 | JWT（角色+scope）；5次失败锁30分钟；**自动解锁+Admin手动解锁** |
| AUTH-02 | Token | P0 | 24h过期；401→登录页；**不维护黑名单**（禁用后Token仍有效至过期） |
| AUTH-03 | 当前用户 | P0 | GET /api/auth/me |
| AUTH-04 | 菜单 | P0 | Admin:组织/员工/号码(scope)；Ops:号码(全局)+号池+导入+区号匹配；Finance:只读；Boss:只读 |
| AUTH-05 | Scope | P0 | WHERE org.path LIKE 'scopePath%' |
| AUTH-06 | 改密 | P1 | 验证旧密码+复杂度 |
| AUTH-07 | 登出 | P0 | 清前端Token；无状态JWT |
| AUTH-08 | 首次登录 | P1 | 强制改密 |

---

### M2：组织架构管理

| ID | 描述 | P | 验收标准 |
|----|------|:--:|----------|
| ORG-01 | 组织树 | P0 | 完整树；scope外置灰+可点击只读；无编辑/删除按钮 |
| ORG-02 | 新增 | P0 | 名称+上级+类型；自动计算path/level；同名校验 |
| ORG-03 | 编辑 | P0 | 名称/类型/状态；不可改上级 |
| ORG-04 | 删除 | P1 | 无子节点+无active员工+无active/stopped号码；根节点+「待分配」不可删 |
| ORG-05 | 类型 | P0 | ENUM: group/subsidiary/dept |
| ORG-06 | 统计 | P1 | 节点显示active员工数+active/stopped号码数 |
| ORG-08 | **深度限制** | P0 | 创建前预校验 path 长度；超过约 25 层 → ORG_DEPTH_EXCEEDED；DB 防御：VARCHAR(1000) |

---

### M3：员工管理

| ID | 描述 | P | 验收标准 |
|----|------|:--:|----------|
| EMP-01 | 列表 | P0 | 分页+搜索+组织过滤；显示当前号码+号码归属组织+号码状态 |
| EMP-02 | 新增 | P0 | 工号+姓名+部门+入职日期（必填）；**自动创建sys_user** |
| EMP-03 | 编辑 | P0 | 可改姓名/部门/职位/电话/邮箱；**工号不可改** |
| EMP-04 | 离职 | P0 | inactive+leave_date；**自动 reclaim 所有号码**（事务联动） |
| EMP-05 | 工号规则 | P0 | 6位；纯数字→auto ext；含字母→manual ext；禁止全0 |
| EMP-06 | 唯一号码 | P0 | 分配/过户前校验 |
| EMP-07 | 复职 | P1 | inactive→active；号码不恢复 |
| EMP-08 | 调动 | P1 | 改org_id；目标在scope内；号码org不联动 |
| EMP-09 | 虚拟员工 | P1 | `VIR-{部门}-{序号}`（如VIR-FRONT-001）；is_virtual=1；manual分机号 |

---

### M4：电话目录管理

#### 4.1 基础 CRUD

| ID | 描述 | P | 验收标准 |
|----|------|:--:|----------|
| PH-01 | 号码列表 | P0 | 分页+6状态过滤+组织过滤+搜索；显示使用人+组织+分机号类型 |
| PH-02 | 号码详情 | P0 | 属性+使用人+组织+**操作历史时间线** |
| PH-03 | 逐条录入 | P0 | Ops；idle入池；全局唯一校验；**自动检测surrender_record→二次入库**；导入时归一化，逐条不归一化 |
| PH-04 | 已拆机列表 | P1 | 分页+日期范围过滤 |
| PH-05 | Excel导入 | P0 | ≤500行；**异步处理**：上传→立即返回 batch_id → 后台逐行解析 → Ops 轮询进度 → 预览 → 确认 → 批量 INSERT（batchSize=100） |
| PH-06 | 导入批次表 | P0 | import_batch（batch_id/file_name/status/total/success/pending/error/uploaded_by） |
| PH-07 | 批量查询优化 | P0 | org 1次全加载 → HashMap；phone 1次 IN 查询 → HashSet；N+1 → 2次查询 |

#### 4.2 Excel 导入流程

```
[Ops 上传] → [逐行解析]
  ├── 号码格式+判重 → 失败标记原因
  ├── 子公司名称匹配 org_structure.name（**精确匹配，Excel列优先**）
  │   ├── 列有值 → 用列值匹配
  │   └── 列为空 → 区号匹配（area_code_org_mapping）
  │       ├── 匹配到唯一 → 自动填入
  │       ├── 匹配到多个 → 按 priority 首选；可选人工调整
  │       └── 无匹配 → 标记"待分配"
  ├── 检测 surrender_record → is_reentry=1
  └── 预览 → Ops确认导入
       └── 待分配行：筛选→排序→批量重分配组织→再次导入
```

#### 4.3 生命周期操作

| ID | 操作 | 状态转移 | P | 锁策略 |
|----|------|----------|:--:|--------|
| PH-10 | allocate | idle→active | P0 | FOR UPDATE 号码+员工 |
| PH-11 | reclaim | active/stopped→idle | P0 | FOR UPDATE 号码 |
| PH-12 | 停机 | active→stopped | P1 | FOR UPDATE 号码 |
| PH-13 | 复机 | stopped→active | P1 | FOR UPDATE 号码 |
| PH-14 | 拆机 | →cancelled | P0 | FOR UPDATE 号码 |
| PH-15 | 过户 | active→active | P1 | FOR UPDATE 号码+新旧员工 |
| PH-16 | 换号 | active→idle+idle→active | P1 | FOR UPDATE **两个号码** |
| PH-17 | 转移 | active→active | P1 | FOR UPDATE 号码 |
| PH-18 | 预留 | idle→reserved | P1 | FOR UPDATE 号码；必须填原因 |
| PH-19 | 解除预留 | reserved→idle | P1 | FOR UPDATE 号码 |
| PH-20 | 禁用 | idle→disabled | P1 | FOR UPDATE 号码；必须填原因 |
| PH-21 | 解除禁用 | disabled→idle | P1 | FOR UPDATE 号码 |

#### 4.4 按钮可见性

| 号码状态 | 可见按钮 |
|----------|---------|
| **idle** | 分配、预留、禁用、拆机 |
| **reserved** | 解除预留、拆机 |
| **disabled** | 解除禁用、拆机 |
| **active** | 回收、停机、过户、换号、转移、拆机 |
| **stopped** | 复机、回收、拆机 |
| **cancelled** | （仅查看+历史） |

#### 4.5 历史与审计

| ID | 描述 | P |
|----|------|:--:|
| PH-30 | 操作历史（phone_history） | P0 |
| PH-31 | 拆机归档（surrender_record） | P0 |
| PH-32 | 操作通知（sys_notification） | P1 |
| PH-33 | 并发保护（乐观锁version+悲观锁FOR UPDATE） | P0 |
| PH-34 | 号码格式校验 | P0 |
| PH-35 | Repository 所有变更操作**必须**使用 `@Lock(PESSIMISTIC_WRITE)` + 专用 `@Query`；**禁止**普通 `findById()` 后做状态变更 | P0 |
| PH-36 | change-number：单一 @Transactional 方法 + 双号码 `findByIdsForUpdate` 一次查询 + `ORDER BY id ASC` 防死锁 | P0 |

---

### M5：分机号池管理

| ID | 描述 | P |
|----|------|:--:|
| EP-01 | 号池列表（按部门+已用/总数） | P1 |
| EP-02 | 新增（6位数字范围+不重叠） | P1 |
| EP-03 | 编辑/删除（删除前校验无使用中分机号） | P1 |
| EP-04 | 自动分配（含字母工号→从部门号池随机；≤100次重试） | P0 |
| EP-05 | 全局唯一校验 | P0 |
| EP-06 | 重叠检测（同部门） | P1 |
| EP-07 | 耗尽提示 `EXT_POOL_EXHAUSTED` | P1 |
| EP-08 | 已用统计（实时） | P1 |
| EP-09 | **三色预警**（绿<60%/黄60-80%/红>80%/黑=100%）；跨 80% 自动通知部门Admin+Ops | P0 |
| EP-10 | 耗尽返回建议扩充范围（`EXT_POOL_EXHAUSTED` 含 orgName+poolUsed/poolTotal+suggestion） | P1 |

---

### M6：区号匹配

| ID | 描述 | P |
|----|------|:--:|
| ACM-01 | 区号列表（Ops管理） | P1 |
| ACM-02 | 新增映射（区号→组织+priority） | P1 |
| ACM-03 | 编辑/删除 | P1 |
| ACM-04 | 导入自动匹配（仅Excel列为空时触发） | P0 |

---

### M7：用户管理

| ID | 描述 | P |
|----|------|:--:|
| USR-01 | 用户列表（Admin scope内） | P1 |
| USR-02 | 创建（BCrypt；自动创建username=employee_no） | P0 |
| USR-03 | 启用/禁用（禁用后无法登录；已有Token仍有效） | P1 |
| USR-04 | 种子数据（admin/ops/boss；`Phonebiz@2026`；强制改密） | P0 |
| USR-05 | 手动解锁（Admin解除锁定） | P1 |

---

### M8：系统通知

| ID | 描述 | P |
|----|------|:--:|
| NOT-01 | 号码操作→通知直属部门Admin+所有Ops；**无Admin时向上查找上级Admin** | P1 |
| NOT-02 | 消息列表（未读数+标已读） | P2 |
| NOT-03 | 仅站内消息 | P0 |
| NOT-04 | **号池预警通知**（跨 80% 阈值时自动推送） | P1 |

---

### M9：Dashboard

| ID | 描述 | P |
|----|------|:--:|
| DSH-01 | 仪表盘（角色+快捷入口+scope/全局号码统计） | P2 |

---

### M10：电话机管理（🆕 新增模块）

> 详见 `PhoneBiz-Phase1-M10-电话机管理.md`

#### 10.1 核心规则

- 话机通过**分机号**与号码绑定（M:N，device_phone_mapping），非直接绑定号码
- MAC 归一化：`A4:B1:C2:D3:E4:F5` → 存储为 `A4B1C2D3E4F5`（大写12位十六进制）
- 一人多话机 / 一话机多人使用
- 设备归属组织，可分配给员工
- 无分机号的号码不可绑定
- 员工离职 → 自动回收话机

#### 10.2 状态机（5 态）

```
stock → active ⇌ inactive
active → repairing → active / retired
any → retired（终态）
```

#### 10.3 需求清单

| ID | 描述 | P | 状态转移 |
|----|------|:--:|----------|
| DEV-01 | 录入话机 | P0 | → stock |
| DEV-02 | 分配话机 | P0 | stock → active |
| DEV-03 | 回收话机 | P1 | active → stock |
| DEV-04 | 停用 | P1 | active/stock → inactive |
| DEV-05 | 恢复 | P1 | inactive → stock |
| DEV-06 | 送修 | P1 | active → repairing |
| DEV-07 | 修复 | P1 | repairing → active |
| DEV-08 | 报废 | P0 | any → retired（终态） |
| DEV-09 | 绑定号码（按分机号） | P1 | — |
| DEV-10 | 解绑号码 | P1 | — |
| DEV-11 | 操作历史 | P0 | 每次状态变更写入 phone_device_history |
| DEV-12 | 离职自动回收 | P0 | 联动 EMP-04 |

#### 10.4 按钮可见性

| 状态 | 可见按钮 |
|------|---------|
| stock | 分配、停用、报废 |
| active | 回收、停用、送修、报废、绑定号码 |
| inactive | 恢复、报废 |
| repairing | 修复、报废 |
| retired | 仅查看+历史 |

---

## 七、权限矩阵

| 功能 | Admin | Ops | Finance | Boss |
|------|:---:|:---:|:---:|:---:|
| 登录/改密 | ✅ | ✅ | ✅ | ✅ |
| 组织查看 | scope(完整树/外置灰) | 全局 | 全局 | 全局（只读） |
| 组织编辑 | scope内 | ❌ | ❌ | ❌ |
| 员工CRUD | scope内 | ❌ | ❌ | ❌ |
| 号码查看 | scope内 | 全局 | 全局 | 全局（只读） |
| 逐条录入 | ❌ | ✅ | ❌ | ❌ |
| Excel导入 | ❌ | ✅ | ❌ | ❌ |
| 分配/回收 | scope内 | ❌ | ❌ | ❌ |
| 预留/禁用 | scope内 | ❌ | ❌ | ❌ |
| 停复机 | scope内 | ✅ | ❌ | ❌ |
| 拆机 | scope内 | ✅ | ❌ | ❌ |
| 过户/换号/转移 | scope内 | ❌ | ❌ | ❌ |
| 号池管理 | ❌ | ✅ | ❌ | ❌ |
| 区号匹配 | ❌ | ✅ | ❌ | ❌ |
| 用户管理 | scope内 | ❌ | ❌ | ❌ |
| 通知 | ✅ | ✅ | ✅ | ✅ |
| 话机查看 | scope 内 | 全局 | 全局 | 全局（只读） |
| 话机录入 | ❌ | ✅ | ❌ | ❌ |
| 话机分配/回收 | scope 内 | ❌ | ❌ | ❌ |
| 话机停用/送修/修复 | scope 内 | ✅ | ❌ | ❌ |
| 话机报废 | scope 内 | ✅ | ❌ | ❌ |
| 话机绑定号码 | scope 内 | ✅ | ❌ | ❌ |
| Dashboard | scope统计 | 全局统计 | 全局统计 | 全局统计 |

> Finance / Boss：**全系统只读**，禁止写操作。Boss/Fin/Admin 不能管理号池和区号匹配。

---

## 八、并发策略（修复版）

| 场景 | 策略 | 死锁预防 |
|------|------|---------|
| allocate | `findByIdForUpdate` 锁号码 + 员工（一次查询） | — |
| reclaim / trouble / surrender / 预留 / 禁用 | `findByIdForUpdate` 锁号码 | — |
| change-user | `findByIdForUpdate` 锁号码 + 新旧员工 | — |
| **change-number** | `findByIdsForUpdate` **双号码一次查询**锁定 | `ORDER BY p.id ASC` 固定锁顺序 |
| 所有变更 | @Transactional(isolation=READ_COMMITTED) + version 二道防线 | — |
| 防并发规则 | **禁止**用普通 `findById()` 后做状态变更；Code Review 强制检查 | — |

---

## 九、错误码

| errorCode | HTTP | 含义 |
|-----------|------|------|
| `EMP_ALREADY_HAS_PHONE` | 400 | 员工已持有号码 |
| `PHONE_NOT_IDLE` | 400 | 号码非空闲 |
| `PHONE_NOT_ACTIVE` | 400 | 号码非在用 |
| `PHONE_ALREADY_EXISTS` | 400 | 号码已存在 |
| `PHONE_IS_SURRENDERED` | 400 | 号码已拆机（提示二次入库） |
| `EXT_POOL_EXHAUSTED` | 400 | 号池耗尽 |
| `EXT_POOL_OVERLAP` | 400 | 号池重叠 |
| `EXT_POOL_REQUIRED` | 400 | 需号池但部门未配置 |
| `EXT_NUMBER_IN_USE` | 400 | 分机号已被使用 |
| `ORG_HAS_CHILDREN` | 400 | 有子节点 |
| `ORG_HAS_EMPLOYEES` | 400 | 有员工 |
| `ORG_NAME_DUPLICATE` | 400 | 同级名称重复 |
| `ORG_DEPTH_EXCEEDED` | 400 | 层级过深 |
| `OUT_OF_SCOPE` | 403 | 超出管理范围 |
| `ACCOUNT_LOCKED` | 401 | 已锁定 |
| `ACCOUNT_DISABLED` | 401 | 已禁用 |
| `TOKEN_EXPIRED` | 401 | Token过期 |
| `FIRST_LOGIN_CHANGE_PWD` | 403 | 首次登录强制改密 |
| `IMPORT_ORG_NOT_FOUND` | 400 | 导入组织匹配失败 |
| `IMPORT_DUPLICATE_IN_BATCH` | 400 | 批次内重复 |
| `DEVICE_MAC_INVALID` | 400 | MAC 格式无效 |
| `DEVICE_MAC_DUPLICATE` | 400 | MAC 已存在 |
| `DEVICE_NOT_STOCK` | 400 | 话机非库存状态 |
| `DEVICE_PHONE_NO_EXTENSION` | 400 | 目标号码无分机号 |

---

## 十、设计决策

| # | 决策 |
|----|------|
| DD-01 | Phase 1 直接操作模式（Phase 2 迁移工单驱动） |
| DD-02 | 分机号双轨：auto（跟人/实时计算/不含已拆机校验）↔ manual（跟号/全局唯一/记录allocation_org_id） |
| DD-03 | 公共号码→虚拟员工（`VIR-{部门}-{序号}`）；is_shared 暂缓 |
| DD-04 | 二次入库自动检测（surrender_record），不设审批 |
| DD-05 | Admin组织树：完整树+scope外置灰+可点击只读 |
| DD-06 | Boss/Finance 全系统只读 |
| DD-07 | 通知仅直属部门Admin → 无则向上查找 + 所有Ops |
| DD-08 | 无状态JWT；30分钟自动+手动解锁；禁用后Token不立撤 |
| DD-09 | **所有状态变更**使用 `@Lock(PESSIMISTIC_WRITE)` + 专用 Repository 方法；禁止普通 findById |
| DD-10 | Excel列优先组织匹配，列空时才走区号匹配 |
| DD-11 | Excel导入**异步+轮询**；批量查询（N+1→2次）；JdbcTemplate batchInsert |
| DD-12 | 员工离职自动回收号码（事务联动） |
| DD-13 | 跨组织分配允许（不校验号码org==员工org） |
| DD-14 | 已拆机号码分机号释放，自动分机号仅校验当前使用中 |
| DD-15 | 预留/禁用仅对idle号码可设置，Admin操作，必须填原因 |
| DD-16 | change-number 双号码一次锁 + ORDER BY id 防死锁 |
| DD-17 | 号池三色预警 + 跨80%阈值自动通知 |
| DD-18 | 组织深度限制（约25层）+ path VARCHAR(1000) |
| DD-19 | **话机与分机号绑定**（非直接绑定号码）；无分机号不可绑定 |
| DD-20 | **员工离职自动回收话机**（与号码回收并列） |
| DD-21 | MAC 归一化：去冒号+转大写+12位十六进制 |

---

## 十一、开放问题

| # | 问题 | Phase 1 处理 |
|---|------|-------------|
| Q1 | 跨组织工单 | 不允许 |
| Q2 | 子公司财务体系 | Phase 3 |
| Q3 | 部署环境 | 末追加docker-compose |
| Q4-7 | 账单/对账/保留/is_shared | Phase 2/3 |

---

*本文档经三轮 35 项 + 电话机 3 项共 38 项逐条用户确认，为 Phase 1 唯一需求依据。*

## 附录：模块概览

| 模块 | 需求ID | 表 | API | 任务 |
|------|--------|:--:|:--:|:--:|
| M1 认证鉴权 | 8 | — | 3 | 6 |
| M2 组织架构 | 7 | org_structure | 5 | 8 |
| M3 员工管理 | 9 | employee | 5 | 7 |
| M4 电话目录 | 25 | phone_number + phone_history + surrender_record | 16 | 20 |
| M5 分机号池 | 10 | extension_pool | 4 | 6 |
| M6 区号匹配 | 4 | area_code_org_mapping | 4 | 3 |
| M7 用户管理 | 5 | sys_user | 3 | 3 |
| M8 系统通知 | 4 | sys_notification | — | 2 |
| M9 Dashboard | 1 | — | 1 | 1 |
| **🆕 M10 电话机管理** | **12** | phone_device + device_phone_mapping + device_device_history | **16** | **12** |
| — 基础设施 | — | import_batch | 4 | 6 |
| — 风险修复 | — | — | — | 12 |
| **合计** | **85** | **19** | **61** | **~84** |
