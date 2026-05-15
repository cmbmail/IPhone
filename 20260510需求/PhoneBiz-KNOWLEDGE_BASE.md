# PhoneBiz 项目知识库

> 生成：2026-05-10 | 4 轮 QA 共 39 项确认 + 5 高风险修复 + 完整文档体系
> 用途：新对话的第一份上下文文件，**先加载此文件再开始工作**

---

## 1. 项目概要

| 项 | 值 |
|----|-----|
| 名称 | PhoneBiz — 企业内线电话业务管理平台 |
| 目录 | `/Users/admin/WorkBuddy/20260510需求/` |
| Phase 1 范围 | 组织架构 + 员工 + 电话全生命周期 + 话机管理 + 分机号池 + Excel导入 + 区号匹配 + 权限 |
| 用户 | Admin(行政) / Ops(运维) / Finance(财务) / Boss(老板) |
| 技术栈 | React 18+Vite+Antd5 / Spring Boot 3.2+JPA+MySQL8+Flyway / JWT(无状态) |
| 团队 | software-phonebiz（齐活林主理 + 许清楚PM + 高见远架构 + 寇豆码工程 + 严过关QA） |
| 数据 | 10 模块 | 85 功能 | 20 表 | 62 API | 26 决策 | 90 任务 | 9 步 | 16 工作日 |
| 迭代 | Phase 2(工单/10d) → Phase 3(费用/10d) | 总计 36 工作日 |

---

## 2. 核心概念速查

### 号码 6 状态机

```
idle ←→ reserved（预留）/ disabled（禁用）
idle → active ⇌ stopped → idle
any → cancelled（不可逆，可二次入库）
```

### 话机 5 状态机

```
stock → active ⇌ inactive
active → repairing → active / retired
any → retired（终态）
```

### 分机号双轨

- **auto（跟人）**：纯数字工号→去前导0；仅校验当前使用中（不含已拆机）；回收后员工保留资格
- **manual（跟号）**：含字母工号/公共号码/人工指定；全局唯一含已拆机；记录 allocation_org_id

### 关键业务规则

| 规则 | 说明 |
|------|------|
| 跨组织分配 | **允许**，不校验 phone.org == employee.org |
| 离职自动回收 | EMP-04 → 号码回收 + 话机回收（独立事务） |
| 组织树 | 完整树展示；scope 外置灰+可点击只读 |
| Boss/Finance | 全系统纯只读，禁止所有写操作 |
| 通知 | 直属部门 Admin（无则向上查找）+ 所有 Ops |
| Excel 导入 | 列优先匹配；列空走区号；失败→"待分配" |
| 二次入库 | 录入时检测 surrender_record → is_reentry=1 |
| 话机绑定 | 按分机号绑定；跨员跨组允许；送修/报废自动解绑 |
| MAC | 归一化去冒号转大写12位十六进制 |
| 并发 | @Lock PESSIMISTIC_WRITE 全部操作；禁止普通 findById |

---

## 3. 20 张数据表（Phase 1）

| 表 | 说明 |
|----|------|
| org_structure | 组织架构（path/level 自动计算，VARCHAR(1000)） |
| employee | 员工（is_virtual 标记） |
| phone_number | 号码主表（+version/+extension_type/+is_reentry/+allocation_org_id） |
| extension_pool | 分机号池 |
| phone_history | 号码操作历史（永久） |
| phone_surrender_record | 拆机归档（永久） |
| phone_device | 电话机主表 |
| device_phone_mapping | 话机-号码关联（M:N） |
| phone_device_history | 话机操作历史 |
| sys_user | 系统用户（+password_changed_at） |
| sys_notification | 系统通知 |
| sys_feature_flag | 功能开关 |
| area_code_org_mapping | 区号-组织对照 |
| import_batch | 导入批次 |
| cost_center_mapping | 成本中心（Phase 3 用，表已建） |
| work_order | 工单主表（Phase 2） |
| work_order_item | 工单项（13类型+6快照字段） |
| phone_snapshot | 月度快照（Phase 2） |
| bill_raw | 账单原始表（Phase 3） |
| bill_allocation | 账单分摊明细（Phase 3） |

---

## 4. 全部设计决策（26 项）

| DD | 决策 |
|----|------|
| DD-01 | Phase 1 直接操作（非工单）；Phase 2 迁移 |
| DD-02 | 分机号双轨：auto(跟人) ↔ manual(跟号) |
| DD-03 | 公共号码→虚拟员工 VIR-{部门}-{序号} |
| DD-04 | 二次入库自动检测 surrender_record |
| DD-05 | Admin 组织树：完整树+scope外置灰+可点击只读 |
| DD-06 | Boss/Finance 全系统只读 |
| DD-07 | 通知：直属部门Admin+向上查找 + 所有Ops |
| DD-08 | 无状态JWT；30分钟自动+手动解锁 |
| DD-09 | 所有状态变更 @Lock PESSIMISTIC_WRITE；禁止普通 findById |
| DD-10 | Excel 列优先匹配；列空走区号 |
| DD-11 | Excel 异步+轮询；批量查询；batchInsert(100) |
| DD-12 | 员工离职自动回收号码（事务联动） |
| DD-13 | 允许跨组织分配 |
| DD-14 | 已拆机分机号释放；auto 仅校验当前使用中 |
| DD-15 | 预留/禁用仅 idle 可设置 |
| DD-16 | change-number 双号码一次锁 + ORDER BY id |
| DD-17 | 号池三色预警 + 跨阈值通知 |
| DD-18 | 组织深度限制(~25层) + path VARCHAR(1000) |
| DD-19 | 话机与分机号绑定；无分机号不可绑定 |
| DD-20 | 离职自动回收话机（与号码并列） |
| DD-21 | MAC 归一化：去冒号+转大写+12位十六进制 |
| DD-22 | 话机-号码跨员工/跨组织绑定允许 |
| DD-23 | 送修自动解绑；surrender/reclaim 不解绑 |
| DD-24 | Flyway 数据库版本化（V1/V2/V3） |
| DD-25 | 模块化包结构（module.{auth,org,phone,device,...}） |
| DD-26 | 功能开关（sys_feature_flag + @ConditionalOnFeatureFlag） |

---

## 5. 并发策略（铁律）

| 场景 | 锁策略 |
|------|--------|
| allocate | `findByIdForUpdate` 锁号码 + 员工 |
| reclaim/trouble/surrender/预留/禁用 | `findByIdForUpdate` 锁号码 |
| change-user | `findByIdForUpdate` 锁号码 + 新旧员工 |
| change-number | `findByIdsForUpdate` 双号码一次锁 + ORDER BY id ASC |
| 所有变更 | @Transactional(READ_COMMITTED) + version 二道防线 |

---

## 6. 权限矩阵速查

| 功能 | Admin | Ops | Finance | Boss |
|------|:---:|:---:|:---:|:---:|
| 组织编辑 | scope | ❌ | ❌ | ❌ |
| 员工CRUD | scope | ❌ | ❌ | ❌ |
| 号码录入/导入 | ❌ | ✅ | ❌ | ❌ |
| 分配/回收/预留/禁用 | scope | ❌ | ❌ | ❌ |
| 停复机 | scope | ✅ | ❌ | ❌ |
| 拆机 | scope | ✅ | ❌ | ❌ |
| 过户/换号/转移 | scope | ❌ | ❌ | ❌ |
| 号池/区号 | ❌ | ✅ | ❌ | ❌ |
| 话机查看 | scope | 全局 | 全局 | 全局(只读) |
| 话机分配/回收 | scope | ❌ | ❌ | ❌ |
| 话机绑定号码 | scope | ✅ | ❌ | ❌ |
| 用户管理 | scope | ❌ | ❌ | ❌ |
| 查看 | scope内 | 全局 | 全局 | 全局(只读) |

---

## 7. 构建路线

```
Phase 1（16d）：9 步
Step1 骨架(2d) → Step2 认证(2d) → Step3 组织员工(3d) → Step4 号码查询(2d)
→ Step5 号码操作(3d) → Step6 号池导入(2d) → Step7 收尾(1d)
→ Step8 话机(2d) → Step9 迭代基础(0.5d)

Phase 2（10d）：工单系统 → 操作迁移为工单驱动
Phase 3（10d）：费用分摊 + 发票 + 报表
──────────────────────────────────────────
总计 36 工作日
```

---

## 8. 已修复的 5 项高风险

| 风险 | 修复 |
|------|------|
| 并发冲突 | @Lock PESSIMISTIC_WRITE 专用方法；禁止普通 findById |
| change-number 断裂 | 单事务+双号码一次锁+ORDER BY 防死锁 |
| 号池耗尽无预警 | 三色预警+跨阈值通知+友好提示 |
| path 溢出 | 深度预校验(~25层)+VARCHAR(1000) |
| 导入超时 | 异步+轮询+批量查询+batchInsert |

---

## 9. 完整文档索引

| 优先级 | 文档 | 用途 |
|:--:|------|------|
| 1 | **PhoneBiz-KNOWLEDGE_BASE.md** | **新对话首先加载** |
| 2 | PhoneBiz-TASKS.md | 90 项任务进度追踪 |
| 3 | PhoneBiz-Phase1-功能清单.md | 85 功能点完整清单 |
| 4 | PhoneBiz-QUICK_REF.md | 一页速查（API+状态机+并发） |
| 5 | PhoneBiz-Phase1-需求清单.md | 完整验收标准 v5.2 |
| 6 | PhoneBiz-Phase1-M10-电话机管理.md | 话机模块详细设计 |
| 7 | PhoneBiz-Phase1-架构设计.md | 系统架构+任务DAG |
| 8 | PhoneBiz数据库设计_DDL.sql | 完整 DDL（20 表） |
| 9 | PhoneBiz-API-v1.0.yaml | OpenAPI 3.0 契约 |
| 10 | PhoneBiz-开发计划与迭代路线.md | 三阶段 36 天里程碑 |
| — | PhoneBiz-Phase1-项目拆解与验证方案.md | 构建路线+21 验证用例 |
| — | PhoneBiz-Phase1-高风险修复方案.md | 5 项风险代码级修复 |
| — | PhoneBiz-迭代架构设计.md | Flyway+模块化+功能开关 |
| — | PhoneBiz-Phase2-工单系统设计.md | 工单（13类型+6快照） |
| — | PhoneBiz-Phase3-费用分摊设计.md | 费用分摊（14功能+6步） |
| — | decisions/README.md | 架构决策索引 |

---

*新对话：加载本文 → 按需加载序号文档 → 不重复上传全部。*
