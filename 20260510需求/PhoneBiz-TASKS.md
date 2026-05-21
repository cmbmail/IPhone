# PhoneBiz Phase 1 — 任务跟踪

> 更新：2026-05-18 | 总任务 ~90 | 状态：✅ Phase 1-3 核心完成

## Step 1：项目骨架（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-001 后端 Gradle + application.yml | ✅ | 2026-05-18 |
| T-002 前端 Vite + React + Antd | ✅ | 2026-05-18 |
| T-003 公共模块（ApiResponse/BaseEntity/ResultCode） | ✅ | 2026-05-18 |
| T-004 异常处理（BusinessException/ErrorCode/GlobalExceptionHandler） | ✅ | 2026-05-18 |
| T-005 DDL 执行（16 表） | ✅ | 2026-05-18 |
| T-005c org_structure.path VARCHAR(1000) 扩展 | ✅ | 2026-05-18 |
| T-033 种子数据（admin/ops/boss + 待分配org） | ✅ | 2026-05-18 |

## Step 2：认证鉴权（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-006 JWT（生成/解析/验证） | ✅ | 2026-05-18 |
| T-007 SecurityConfig + CorsConfig + JwtFilter | ✅ | 2026-05-18 |
| T-008 UserDetailsServiceImpl | ✅ | 2026-05-18 |
| T-009 AuthController + AuthService（login/logout/me） | ✅ | 2026-05-18 |
| T-010 前端 authStore + api/client + axios 拦截器 | ✅ | 2026-05-18 |
| T-011 登录锁定 + 手动解锁 + 首次强制改密 | ✅ | 2026-05-18 |

## Step 3：组织+员工（3天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-012 Org entity + repository | ✅ | 2026-05-18 |
| T-013 OrgService + OrgController | ✅ | 2026-05-18 |
| T-014 TreeBuilder | ✅ | 2026-05-18 |
| T-012b path/level 自动计算 | ✅ | 2026-05-18 |
| T-012d depth 预校验（~25层） | ✅ | 2026-05-18 |
| T-012c 组织约束校验（同名/子节点/环/inactive） | ✅ | 2026-05-18 |
| T-015 PermissionEvaluator（scope 裁剪） | ✅ | 2026-05-18 |
| T-016 前端 OrgTree（全局+置灰+只读点击） | ✅ | 2026-05-18 |
| T-017 Employee entity + repository + service | ✅ | 2026-05-18 |
| T-018 EmployeeController + DTO | ✅ | 2026-05-18 |
| T-016b 员工创建联动 SysUser | ✅ | 2026-05-18 |
| T-016c 员工字段校验（手机/邮箱/工号） | ✅ | 2026-05-18 |
| T-019 前端 EmployeePage（表格+搜索+表单） | ✅ | 2026-05-18 |

## Step 4：号码查询（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-020 Phone entity（含 version/extension_type/is_reentry/allocation_org_id） | ✅ | 2026-05-18 |
| T-021 PhoneHistory + PhoneSurrenderRecord entity | ✅ | 2026-05-18 |
| T-022 PhoneRepository（列表/搜索/状态过滤） | ✅ | 2026-05-18 |
| T-023 PhoneService（list/get/history）+ PhoneController GET | ✅ | 2026-05-18 |
| T-024 前端 PhoneTable（6状态过滤+搜索） | ✅ | 2026-05-18 |
| T-025 前端 PhoneDetail（属性+历史时间线） | ✅ | 2026-05-18 |
| T-026 前端 PhoneStatusBadge（6色标签） | ✅ | 2026-05-18 |

## Step 5：号码操作（3天）⚠️ 并发关键

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-020d PhoneRepository 专用锁方法（findByIdForUpdate / findByIdsForUpdate） | ✅ | 2026-05-18 |
| T-027 allocate（完整校验链+悲观锁+分机号处理） | ✅ | 2026-05-18 |
| T-028 reclaim（双轨回收+allocation_org_id归还） | ✅ | 2026-05-18 |
| T-029 trouble（停机/复机） | ✅ | 2026-05-18 |
| T-030 surrender（归档+二次确认+re-entry检测） | ✅ | 2026-05-18 |
| T-031 change-user（分机号按新员工重新判定） | ✅ | 2026-05-18 |
| T-024d change-number（单事务+双号码一次锁+ORDER BY） | ✅ | 2026-05-18 |
| T-032 change-org（转移） | ✅ | 2026-05-18 |
| T-033 reserve/release + disable/enable | ✅ | 2026-05-18 |
| T-034 EmployeeNoValidator + PhoneNumberGenerator | ✅ | 2026-05-18 |
| T-034a EMP-04 离职自动 reclaim | ✅ | 2026-05-18 |
| T-035 NotificationService（号码操作→通知） | ✅ | 2026-05-18 |
| T-025c 号池预警通知 NOT-04 | ✅ | 2026-05-18 |
| T-036 前端所有操作 Dialog（9种×表单+按钮可见性） | ✅ | 2026-05-18 |

## Step 6：号池+导入（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-037 ExtensionPool CRUD + 重叠检测 | ✅ | 2026-05-18 |
| T-029c 号池用量计算 + 三色预警组件 | ✅ | 2026-05-18 |
| T-029d 阈值跨越检测 + 自动通知 | ✅ | 2026-05-18 |
| T-038 AreaCodeOrgMapping CRUD | ✅ | 2026-05-18 |
| T-030a import_batch 表 + entity | ✅ | 2026-05-18 |
| T-030b ImportService（异步+批量查询+归一化+判重+surrender检测） | ✅ | 2026-05-18 |
| T-030c 批量查询优化（HashMap+IN查询） | ✅ | 2026-05-18 |
| T-030d 批量 INSERT（batchSize=100） | ✅ | 2026-05-18 |
| T-030e 导入进度轮询 API | ✅ | 2026-05-18 |
| T-031 前端导入页（上传→进度条→预览→确认） | ✅ | 2026-05-18 |

## Step 7：收尾部署（1天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-039 Dashboard API + 前端 | ✅ | 2026-05-18 |
| T-040 全局 @PreAuthorize + 前端按钮权限 | ✅ | 2026-05-18 |
| T-041 通知消息列表 | ✅ | 2026-05-18 |
| T-042 vite proxy config | ✅ | 2026-05-18 |
| T-043 docker-compose.yml | ✅ | 2026-05-18 |
| T-044 全流程端到端测试 | ✅ | 2026-05-18 |

---

## 🆕 Step 8：电话机管理（新增模块，2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-060 phone_device DDL（3表） | ✅ | 2026-05-18 |
| T-061 Device entity + repository | ✅ | 2026-05-18 |
| T-062 DeviceService（CRUD+MAC归一化+状态变更+历史） | ✅ | 2026-05-18 |
| T-063 DeviceController（16端点） | ✅ | 2026-05-18 |
| T-064 话机列表页（分页+状态过滤+组织过滤） | ✅ | 2026-05-18 |
| T-065 话机详情页（信息+绑定号码+历史） | ✅ | 2026-05-18 |
| T-066 话机表单（录入/编辑） | ✅ | 2026-05-18 |
| T-067 分配/回收/停用/送修/修复/报废 操作弹窗 | ✅ | 2026-05-18 |
| T-068 绑定/解绑号码弹窗 | ✅ | 2026-05-18 |
| T-069 话机按钮可见性（5状态） | ✅ | 2026-05-18 |
| T-070 话机操作通知 | ✅ | 2026-05-18 |
| T-071 员工离职时自动回收分配的话机 | ✅ | 2026-05-18 |

---

## 🆕 Step 9：迭代基础设施（0.5天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-080 Flyway 依赖 + application.yml | ✅ | 2026-05-18 |
| T-081 DDL 迁移 V1__phase1_core.sql | ✅ | 2026-05-18 |
| T-082 module/ 子包重排 | ✅ | 2026-05-18 |
| T-083 sys_feature_flag DDL + seed | ✅ | 2026-05-18 |
| T-084 @ConditionalOnFeatureFlag 注解 | ✅ | 2026-05-18 |
| T-085 FeatureFlagService + /api/flags | ✅ | 2026-05-18 |

---

## 🆕 Phase 2：工单系统（10天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-086 work_order + work_order_item DDL | ✅ | 2026-05-18 |
| T-087 WorkOrder CRUD + 状态流转 | ✅ | 2026-05-18 |
| T-088 工单创建（Admin页面发起13种类型） | ✅ | 2026-05-18 |
| T-089 工单处理（Ops接收→处理→完成） | ✅ | 2026-05-18 |
| T-090 号码操作迁移至工单驱动 | ✅ | 2026-05-18 |
| T-091 批量工单（batch_id拆分） | ✅ | 2026-05-18 |
| T-092 工单通知 + 前端页面 | ✅ | 2026-05-18 |

---

## 🆕 Phase 3：费用管理（10天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-093 成本中心对照表 CRUD | ✅ | 2026-05-18 |
| T-094 月度快照自动生成 | ✅ | 2026-05-18 |
| T-095 账单Excel导入 | ✅ | 2026-05-18 |
| T-096 自动分摊匹配引擎 | ✅ | 2026-05-18 |
| T-097 异常检测引擎 | ✅ | 2026-05-18 |
| T-098 6步确认流水线 | ✅ | 2026-05-18 |
| T-099 子公司对账 | ✅ | 2026-05-18 |
| T-100 发票OCR识别+上传+分发 | ✅ | 2026-05-18 |
| T-101 报表（号码资产+分摊汇总+工单统计+异常账单） | ✅ | 2026-05-18 |

---

## 🆕 Phase 4：工单系统完善（新增任务）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-102 WorkOrderDrivenPhoneController | ✅ | 2026-05-18 |
| T-103 WorkOrderDrivenDeviceController | ✅ | 2026-05-18 |
| T-104 WorkOrderDrivenPhoneService | ✅ | 2026-05-18 |
| T-105 WorkOrderDrivenDeviceService | ✅ | 2026-05-18 |
| T-106 功能开关 sys_feature_flag 集成 | ✅ | 2026-05-18 |
| T-107 话机工单驱动（独立Controller） | ✅ | 2026-05-18 |

---

**状态图例**：⬜ 待开始 | 🔄 进行中 | ✅ 已完成 | ❌ 阻塞 | ⏭️ 跳过

**总任务**：~90 → **~107**
**完成率**：**100%** ✅

---

**完成率**：**100%** ✅