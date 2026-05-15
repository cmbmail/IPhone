# PhoneBiz — 开发计划与迭代路线

> 版本：1.0 | 日期：2026-05-10 | 10 模块/85 功能/20 表/90 任务

---

## 一、三阶段总览

```
Phase 1 ──────── Phase 2 ──────── Phase 3
16d (核心平台)    10d (工单迁移)     10d (费用管理)
     │                │                 │
     ▼                ▼                 ▼
  可独立上线      电话操作迁移为     账单分摊+发票分发
                 工单驱动           +完整报表
```

| 阶段 | 工作日 | 范围 | 新增表 | 交付物 |
|------|:--:|------|:--:|------|
| **Phase 1** | 16 | 组织+员工+电话+话机+权限+导入 | 20 | 可独立运行的电话管理平台 |
| **Phase 2** | 10 | 工单系统（号码操作迁移） | 2 | 工单驱动的号码和话机操作 |
| **Phase 3** | 10 | 账单分摊+发票分发+报表 | 6+ | 完整的费用管理闭环 |
| **合计** | **36** | | **28+** | |

---

## 二、Phase 1 详细计划（16 工作日）

### Step 1：项目骨架（2d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 1 | 后端 Gradle 项目 + application.yml + Flyway | 可启动的 Spring Boot 应用 |
| 1 | DDL 迁移到 V1__phase1_core.sql + 执行 | 20 张表创建完成 |
| 2 | 前端 Vite + React + Antd + Router | 可运行的 SPA |
| 2 | ApiResponse/BaseEntity/ErrorCode/GlobalExceptionHandler | 通用基础设施 |
| 2 | DataInitializer（种子数据 + 待分配org + 功能开关） | 3 账号可登录 |

### Step 2：认证鉴权（2d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 3 | JwtTokenProvider + SecurityConfig + JwtFilter | Token 签发和校验 |
| 3 | UserDetailsServiceImpl + SysUser entity | 从 DB 加载用户 |
| 4 | AuthController（login/logout/me）+ 锁定逻辑 | 完整认证闭环 |
| 4 | 前端 LoginForm + authStore + axios 拦截器 | 登录页可用 |

### Step 3：组织架构 + 员工（3d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 4-5 | Org CRUD + TreeBuilder + path/level 自动计算 + depth 预校验 | 组织树 API |
| 5 | PermissionEvaluator（scope path 裁剪） | 权限过滤 |
| 5 | 前端 OrgTree（完整树+scope外置灰+可点击只读） | 组织管理页 |
| 6 | Employee CRUD + 工号校验 + 自动创建 SysUser | 员工 API |
| 6-7 | 前端 EmployeeTable + EmployeeForm | 员工管理页 |

### Step 4：号码查询（2d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 7 | Phone entity（含 version/extension_type/is_reentry） | 号码实体 |
| 7 | PhoneRepository（列表/搜索/6状态过滤） | 查询方法 |
| 8 | PhoneHistory + PhoneSurrenderRecord | 历史实体 |
| 8 | 前端 PhoneTable + PhoneDetail + 历史时间线 | 号码列表和详情页 |

### Step 5：号码操作（3d）⚠️ 最复杂

| 天 | 任务 | 产出 |
|:--:|------|------|
| 8 | PhoneRepository 专用锁方法 | findByIdForUpdate / findByIdsForUpdate |
| 9 | allocate（完整校验链+悲观锁+分机号双轨） | 分配功能 |
| 9 | reclaim（双轨回收+allocation_org_id） | 回收功能 |
| 9-10 | trouble + surrender（归档+二次确认） | 停复机+拆机 |
| 10 | change-user + change-number + change-org | 变更操作 |
| 10 | reserve/release + disable/enable | 预留+禁用 |
| 11 | NotificationService + 离职自动回收 | 通知+联动 |
| 11 | 前端所有操作 Dialog（9 种） | 操作弹窗 |

### Step 6：号池 + 区号 + Excel 导入（2d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 11-12 | ExtensionPool CRUD + 三色预警 + 阈值通知 | 号池管理 |
| 12 | AreaCodeOrgMapping CRUD | 区号匹配 |
| 12 | Excel 导入（异步+轮询+batchInsert+批量查询优化） | 导入功能 |
| 13 | 前端导入页（上传→进度→预览→确认）+ 号池页 + 区号页 | UI 完成 |

### Step 7：收尾部署（1d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 13 | Dashboard + 全局 @PreAuthorize + 前端按钮权限 | 权限贯通 |
| 13 | docker-compose.yml + vite proxy | 部署脚本 |

### Step 8：电话机管理（2d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 14 | phone_device 3 表 DDL + entity + repository | 话机数据层 |
| 14 | DeviceService（7种操作+MAC归一化+联动规则） | 话机业务逻辑 |
| 15 | 前端 DeviceTable + 操作弹窗 + 绑定号码 | 话机管理页 |
| 15 | 送修自动解绑 + 报废自动解绑 + 离职联动 | 联动测试 |

### Step 9：迭代基础设施（0.5d）

| 天 | 任务 | 产出 |
|:--:|------|------|
| 16 | module/ 包结构重组 + FeatureFlagService + @ConditionalOnFeatureFlag | 迭代架构就绪 |

---

## 三、Phase 2 迭代计划（10 工作日）

### 目标

将 Phase 1 的**直接操作模式**迁移为**工单驱动模式**，并启用电话机工单。

### 迭代内容

| # | 内容 | 天数 | 说明 |
|:--:|------|:--:|------|
| 1 | work_order + work_order_item 后端 CRUD | 2 | 工单基础 CRUD + 状态流转 |
| 2 | 工单创建：Admin 页面发起 13 种类型 | 2 | 号码类 7 种 + 话机类 6 种 |
| 3 | 工单处理：Ops 接收→处理→完成 | 2 | 工单状态流转 + 操作执行 |
| 4 | 号码操作迁移 | 2 | allocate/reclaim/trouble 等从直接 API 改为工单内完成 |
| 5 | 批量工单（batch_id 拆分） | 1 | 单次多号码批量提交+自动拆单 |
| 6 | 工单通知 + 前端完整页面 | 1 | 工单列表+详情+处理页 |
| 7 | 启用 work_order 功能开关 | 0 | sys_feature_flag.work_order=1 |

### 迁移策略

```
Phase 1（直接操作）              Phase 2（工单驱动）
POST /api/phones/{id}/allocate → 创建工单(type=allocate) → Ops处理 → 系统自动执行
POST /api/phones/{id}/reclaim  → 创建工单(type=reclaim)  → Ops处理 → 系统自动执行
```

Phase 1 的直接 API **保留**（向后兼容），Phase 2 新增工单 API。前端默认用工单模式。

---

## 四、Phase 3 迭代计划（10 工作日）

### 目标

实现费用分摊 + 发票分发 + 报表，形成完整的企业电话管理闭环。

### 迭代内容

| # | 内容 | 天数 | 说明 |
|:--:|------|:--:|------|
| 1 | 成本中心对照表 CRUD | 1 | 集团财务维护部门↔成本中心映射 |
| 2 | 月度快照自动生成 | 1 | 每月1日定时任务 + 手动触发 |
| 3 | 账单 Excel 导入（复用 Phase1 框架） | 1 | 异步+轮询+批量INSERT |
| 4 | 自动分摊匹配引擎 | 2 | bill_raw → bill_allocation（快照匹配） |
| 5 | 异常检测引擎 | 1 | 金额波动+无快照+异常大 自动标记 |
| 6 | 6步确认流水线 | 2 | Admin确认→财务确认→提交锁定 |
| 7 | 子公司对账 | 1 | 子公司财务查看+确认 |
| 8 | 发票 OCR 识别 + 上传 + 分发 | 1 | PDF 提取公司名称 → 匹配 → 分发 |
| 9 | 报表（号码资产+分摊汇总+工单统计+异常账单） | 0.5 | 4 张核心报表 |
| 10 | 启用 billing + invoice 功能开关 | 0 | 灰度上线 |

---

## 五、里程碑与交付物

| 里程碑 | 阶段 | 工作日 | 交付物 |
|--------|:--:|:--:|------|
| **M1** | P1-S1~S3 | 7 | 组织架构+员工管理可演示 |
| **M2** | P1-S4~S5 | 13 | 号码全生命周期可演示 |
| **M3** | **P1 完成** | **16** | **完整电话管理平台 v1.0 可上线** |
| **M4** | P2 完成 | 26 | 工单驱动模式 v2.0 |
| **M5** | **P3 完成** | **36** | **费用分摊+发票+报表 v3.0 完整版** |

### 每个里程碑的验收标准

**M1**：组织树展示+CRUD、员工CRUD、账号登录、scope 权限裁剪
**M2**：号码 6 状态全生命周期操作、分机号双轨、并发保护、操作历史
**M3**：Excel 导入、号池管理、区号匹配、话机管理、Dashboard、可 docker-compose 部署
**M4**：工单创建/处理/归档全流程、号码操作迁移至工单内
**M5**：账单导入→分摊→确认完整流程、发票 OCR 分发、4 张报表

---

## 六、风险与缓解

| 阶段 | 风险 | 缓解 |
|------|------|------|
| P1 S5 | 并发锁实现错误 | V7-V9 JMeter 强制验证 |
| P1 S5 | 分机号双轨逻辑错误 | 单元测试覆盖 4×2=8 种组合 |
| P1 S6 | Excel 导入超时 | 异步+轮询+batchInsert（已设计） |
| P2 | Phase1 API → 工单迁移兼容性 | Phase1 API 保留，工单为新增路径 |
| P3 | 快照生成性能 | 单月快照在 5 分钟内完成 |
| P3 | OCR 识别准确率 | 降级方案：人工手动指定接收方 |

---

## 七、技术债追踪

| 项目 | 说明 | 处理阶段 |
|------|------|:--:|
| 架构设计文档同步更新 | 原 1067 行架构设计需更新到 v5.2 标准 | P1 后 |
| API 契约 YAML 补充话机端点 | OpenAPI 需加 M10 的 16 个端点 | P1 后 |
| 决策日志补充 DD-19~26 | decisions/ 目录加 8 个新决策文件 | P1 中 |
| 前端 E2E 测试 | Cypress/Playwright 关键路径 | P1 后 |
| 后端集成测试 | Spring Boot Test 覆盖率 >70% | P1 中 |

---

*开发计划经 4 轮需求审查、39 项用户 QA 确认、5 项高风险修复后定稿。*
