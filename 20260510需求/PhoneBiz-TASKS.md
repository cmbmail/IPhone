# PhoneBiz Phase 1 — 任务跟踪

> 更新：2026-05-10 | 总任务 ~72 | 状态：⏳ 待启动

## Step 1：项目骨架（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-001 后端 Gradle + application.yml | ⬜ | — |
| T-002 前端 Vite + React + Antd | ⬜ | — |
| T-003 公共模块（ApiResponse/BaseEntity/ResultCode） | ⬜ | — |
| T-004 异常处理（BusinessException/ErrorCode/GlobalExceptionHandler） | ⬜ | — |
| T-005 DDL 执行（16 表） | ⬜ | — |
| T-005c org_structure.path VARCHAR(1000) 扩展 | ⬜ | — |
| T-033 种子数据（admin/ops/boss + 待分配org） | ⬜ | — |

## Step 2：认证鉴权（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-006 JWT（生成/解析/验证） | ⬜ | — |
| T-007 SecurityConfig + CorsConfig + JwtFilter | ⬜ | — |
| T-008 UserDetailsServiceImpl | ⬜ | — |
| T-009 AuthController + AuthService（login/logout/me） | ⬜ | — |
| T-010 前端 authStore + api/client + axios 拦截器 | ⬜ | — |
| T-011 登录锁定 + 手动解锁 + 首次强制改密 | ⬜ | — |

## Step 3：组织+员工（3天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-012 Org entity + repository | ⬜ | — |
| T-013 OrgService + OrgController | ⬜ | — |
| T-014 TreeBuilder | ⬜ | — |
| T-012b path/level 自动计算 | ⬜ | — |
| T-012d depth 预校验（~25层） | ⬜ | — |
| T-012c 组织约束校验（同名/子节点/环/inactive） | ⬜ | — |
| T-015 PermissionEvaluator（scope 裁剪） | ⬜ | — |
| T-016 前端 OrgTree（全局+置灰+只读点击） | ⬜ | — |
| T-017 Employee entity + repository + service | ⬜ | — |
| T-018 EmployeeController + DTO | ⬜ | — |
| T-016b 员工创建联动 SysUser | ⬜ | — |
| T-016c 员工字段校验（手机/邮箱/工号） | ⬜ | — |
| T-019 前端 EmployeePage（表格+搜索+表单） | ⬜ | — |

## Step 4：号码查询（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-020 Phone entity（含 version/extension_type/is_reentry/allocation_org_id） | ⬜ | — |
| T-021 PhoneHistory + PhoneSurrenderRecord entity | ⬜ | — |
| T-022 PhoneRepository（列表/搜索/状态过滤） | ⬜ | — |
| T-023 PhoneService（list/get/history）+ PhoneController GET | ⬜ | — |
| T-024 前端 PhoneTable（6状态过滤+搜索） | ⬜ | — |
| T-025 前端 PhoneDetail（属性+历史时间线） | ⬜ | — |
| T-026 前端 PhoneStatusBadge（6色标签） | ⬜ | — |

## Step 5：号码操作（3天）⚠️ 并发关键

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-020d PhoneRepository 专用锁方法（findByIdForUpdate / findByIdsForUpdate） | ⬜ | — |
| T-027 allocate（完整校验链+悲观锁+分机号处理） | ⬜ | — |
| T-028 reclaim（双轨回收+allocation_org_id归还） | ⬜ | — |
| T-029 trouble（停机/复机） | ⬜ | — |
| T-030 surrender（归档+二次确认+re-entry检测） | ⬜ | — |
| T-031 change-user（分机号按新员工重新判定） | ⬜ | — |
| T-024d change-number（单事务+双号码一次锁+ORDER BY） | ⬜ | — |
| T-032 change-org（转移） | ⬜ | — |
| T-033 reserve/release + disable/enable | ⬜ | — |
| T-034 EmployeeNoValidator + PhoneNumberGenerator | ⬜ | — |
| T-034a EMP-04 离职自动 reclaim | ⬜ | — |
| T-035 NotificationService（号码操作→通知） | ⬜ | — |
| T-025c 号池预警通知 NOT-04 | ⬜ | — |
| T-036 前端所有操作 Dialog（9种×表单+按钮可见性） | ⬜ | — |

## Step 6：号池+导入（2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-037 ExtensionPool CRUD + 重叠检测 | ⬜ | — |
| T-029c 号池用量计算 + 三色预警组件 | ⬜ | — |
| T-029d 阈值跨越检测 + 自动通知 | ⬜ | — |
| T-038 AreaCodeOrgMapping CRUD | ⬜ | — |
| T-030a import_batch 表 + entity | ⬜ | — |
| T-030b ImportService（异步+批量查询+归一化+判重+surrender检测） | ⬜ | — |
| T-030c 批量查询优化（HashMap+IN查询） | ⬜ | — |
| T-030d 批量 INSERT（batchSize=100） | ⬜ | — |
| T-030e 导入进度轮询 API | ⬜ | — |
| T-031 前端导入页（上传→进度条→预览→确认） | ⬜ | — |

## Step 7：收尾部署（1天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-039 Dashboard API + 前端 | ⬜ | — |
| T-040 全局 @PreAuthorize + 前端按钮权限 | ⬜ | — |
| T-041 通知消息列表 | ⬜ | — |
| T-042 vite proxy config | ⬜ | — |
| T-043 docker-compose.yml | ⬜ | — |
| T-044 全流程端到端测试 | ⬜ | — |

---

## 🆕 Step 8：电话机管理（新增模块，2天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-060 phone_device DDL（3表） | ⬜ | — |
| T-061 Device entity + repository | ⬜ | — |
| T-062 DeviceService（CRUD+MAC归一化+状态变更+历史） | ⬜ | — |
| T-063 DeviceController（16端点） | ⬜ | — |
| T-064 话机列表页（分页+状态过滤+组织过滤） | ⬜ | — |
| T-065 话机详情页（信息+绑定号码+历史） | ⬜ | — |
| T-066 话机表单（录入/编辑） | ⬜ | — |
| T-067 分配/回收/停用/送修/修复/报废 操作弹窗 | ⬜ | — |
| T-068 绑定/解绑号码弹窗 | ⬜ | — |
| T-069 话机按钮可见性（5状态） | ⬜ | — |
| T-070 话机操作通知 | ⬜ | — |
| T-071 员工离职时自动回收分配的话机 | ⬜ | — |

---

**状态图例**：⬜ 待开始 | 🔄 进行中 | ✅ 已完成 | ❌ 阻塞 | ⏭️ 跳过

**总任务**：~72 → **~84**

## 🆕 Step 9：迭代基础设施（0.5天）

| 任务 | 状态 | 完成日期 |
|------|:--:|------|
| T-080 Flyway 依赖 + application.yml | ⬜ | — |
| T-081 DDL 迁移 V1__phase1_core.sql | ⬜ | — |
| T-082 module/ 子包重排 | ⬜ | — |
| T-083 sys_feature_flag DDL + seed | ⬜ | — |
| T-084 @ConditionalOnFeatureFlag 注解 | ⬜ | — |
| T-085 FeatureFlagService + /api/flags | ⬜ | — |

---

**状态图例**：⬜ 待开始 | 🔄 进行中 | ✅ 已完成 | ❌ 阻塞 | ⏭️ 跳过

**总任务**：~84 → **~90**
