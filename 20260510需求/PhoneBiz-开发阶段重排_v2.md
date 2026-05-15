# PhoneBiz 开发阶段重排 v2 — 按数据依赖拆解

> 生成：2026-05-14 | 基于 PhoneBiz 系统建设实施方案 + 现有 DDL 24 表
> 原则：**每个模块只建自己需要的表，只写自己需要的代码，严格按数据依赖自底向上**

---

## 依赖顺序总览（27 模块）

```
M01 项目骨架
 └─ M02 组织架构 (org_structure)
     ├─ M03 员工管理 (employee)
     │   ├─ M04 认证授权 (sys_user + JWT) ← 此后所有模块需要认证
     │   └─ (M03 也支撑 M09-M12 的员工关联)
     ├─ M05 成本中心 (cost_center_mapping) ← 无其他模块依赖，可随时开发
     └─ M06 号码基础 (phone_number + phone_history + phone_surrender_record)
         ├─ M07 分机号池 (extension_pool)
         ├─ M08 区号对照 (area_code_org_mapping)
         ├─ M09 号码分配/回收 (allocate + reclaim) ⚠️ 并发关键
         ├─ M10 号码状态变更 (reserve/release/disable/enable/trouble/surrender)
         ├─ M11 号码变更 (change-user/change-number/change-org + 离职回收)
         ├─ M12 号码导入 (import_batch + Excel异步)
         └─ M13 话机基础 (phone_device + device_phone_mapping + phone_device_history)
             └─ M14 话机操作 (分配/回收/停用/送修/修复/报废/绑定/解绑)

M15 通知系统 (sys_notification) ← 依赖 M09-M14 操作触发
M16 权限收尾 (@PreAuthorize + 前端按钮 + 组织树置灰)
M17 仪表盘+功能开关 (Dashboard + sys_feature_flag)

═══════════ Phase 2 ═══════════

M18 工单基础 (work_order + work_order_item) ← 依赖 M06
M19 工单流转+批量拆单 ← 依赖 M18
M20 号码操作迁移工单驱动 ← 依赖 M18+M09+M10+M11
M21 月度快照 (phone_snapshot + 定时任务) ← 依赖 M06+M02
M22 基础报表 (号码资产 + 工单统计) ← 依赖 M06+M18+M21

═══════════ Phase 3 ═══════════

M23 账单导入+分摊 (bill_raw + bill_allocation) ← 依赖 M21+M05+M02
M24 异常检测 (历史对比 + 阈值) ← 依赖 M23
M25 发票上传+OCR (invoice + invoice_file) ← 依赖 M02
└─ M26 发票分发+确认 (invoice_distribution + subsidiary_reconciliation) ← 依赖 M25
M27 完整报表 (分摊/发票/异常) ← 依赖 M22+M23+M24+M26
```

---

## Phase 1：基础平台（M01-M17）

### M01 — 项目骨架

| 维度 | 内容 |
|------|------|
| **依赖** | 无 |
| **新建表** | 无（仅建 Flyway 基线） |
| **后端** | Gradle 项目 + application.yml + ApiResponse/BaseEntity/ResultCode + GlobalExceptionHandler + CorsConfig + Flyway 配置 |
| **前端** | Vite + React + Antd + axios client + 路由骨架 + vite proxy |
| **验收** | `gradle build` 成功，前端 `npm run dev` 可启动，Flyway 连接 MySQL 成功 |

> 不复用旧代码。从零搭建干净骨架。

---

### M02 — 组织架构

| 维度 | 内容 |
|------|------|
| **依赖** | M01 |
| **新建表** | `org_structure` (parent_id + type + level + path + status) |
| **后端** | Org entity + repository(含 findByParentId/findByPath) + OrgService(CRUD) + TreeBuilder(递归构建树) + OrgController(5端点) + path/level 自动计算 + depth 预校验(~25层) + 约束校验(同名/子节点/环/inactive) |
| **前端** | OrgTree 组件(Antd Tree) + 组织表单(新增/编辑 Dialog) |
| **种子数据** | 集团总部 org + 待分配部门 org |
| **验收** | 树形展示正确，增删改查正常，同名/环检测生效 |

---

### M03 — 员工管理

| 维度 | 内容 |
|------|------|
| **依赖** | M02 |
| **新建表** | `employee` (employee_no UNIQUE + org_id FK + status + entry_date/leave_date) |
| **后端** | Employee entity + repository + EmployeeService(CRUD) + EmployeeController + 工号校验(6位字符规则) + 字段校验(手机/邮箱) + 虚拟员工(VIR-{部门}-{序号}) |
| **前端** | EmployeePage(ProTable 分页+搜索+状态过滤) + EmployeeForm(新增/编辑 Drawer) |
| **种子数据** | VIR-总部-01(admin), VIR-总部-02(ops), VIR-总部-03(boss) |
| **验收** | 工号唯一性校验生效，虚拟员工创建无误，离职 date 记录 |

---

### M04 — 认证授权

| 维度 | 内容 |
|------|------|
| **依赖** | M03 (sys_user FK→employee) |
| **新建表** | `sys_user` (username UNIQUE + password_hash + role ENUM + scope_org_id + status + password_changed_at + login_fail_count + locked_until) |
| **后端** | JWT 工具类(生成/解析/验证) + SecurityConfig + JwtFilter + UserDetailsServiceImpl + AuthController(login/logout/me) + 登录锁定(5次失败锁定30分钟) + 手动解锁 + 首次强制改密 |
| **前端** | 登录页 + authStore(Zustand) + axios 拦截器(自动附加 token + 401跳转) + 路由守卫 |
| **种子数据** | admin/VIR-总部-01, ops/VIR-总部-02, boss/VIR-总部-03 (初始密码需强制改) |
| **验收** | 登录/登出正常，JWT 过期跳转，锁定/解锁/改密流程完整 |

---

### M05 — 成本中心

| 维度 | 内容 |
|------|------|
| **依赖** | M02 + M04 |
| **新建表** | `cost_center_mapping` (org_id FK + cost_center_code UNIQUE + status) |
| **后端** | CostCenter entity + repository + CostCenterService(CRUD) + 权限控制(仅集团 Finance 可写) |
| **前端** | CostCenterPage(ProTable + 表单) |
| **验收** | 仅 Finance 角色可增删改，多对多关联正常 |

> 小模块，半天开发量。与 M04 并行开发无冲突。

---

### M06 — 号码基础

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 (phone.org_id FK) |
| **新建表** | `phone_number` (+version +extension_type +is_reentry +allocation_org_id) + `phone_history` (永久) + `phone_surrender_record` (永久) |
| **后端** | Phone entity + PhoneHistory entity + SurrenderRecord entity + PhoneRepository(列表/搜索/状态过滤) + PhoneService(list/get/history) + PhoneController(GET only, 5端点) |
| **前端** | PhoneTable(ProTable + 6状态过滤 + 搜索) + PhoneDetail(属性卡片 + 历史时间线) + PhoneStatusBadge(6色标签) |
| **验收** | 列表分页/搜索/过滤正常，详情页历史时间线完整，状态标签颜色正确 |

---

### M07 — 分机号池

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 |
| **新建表** | `extension_pool` (org_id FK + start_number + end_number) |
| **后端** | ExtensionPool entity + repository + ExtensionPoolService(CRUD) + 重叠检测(start/end 范围校验) + 用量计算(当前已分配数/池大小) + 三色预警(绿≥30%/黄10-30%/红<10%) + 阈值跨越通知 |
| **前端** | ExtensionPoolPage(ProTable + 配置表单) + 预警组件(三色进度条) |
| **验收** | 重叠检测拒绝非法范围，预警颜色正确，跨越阈值触发通知 |

> Ops 独占操作，Admin 可见只读。

---

### M08 — 区号对照

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 |
| **新建表** | `area_code_org_mapping` (area_code + org_id FK) |
| **后端** | AreaCodeOrgMapping entity + repository + CRUD |
| **前端** | AreaCodePage(ProTable + 表单) |
| **验收** | 区号-组织映射增删改查正常 |

> 小模块，半天开发量。与 M07 并行开发无冲突。

---

### M09 — 号码分配/回收 ⚠️ 并发关键

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M07 + M08 + M03 |
| **后端** | PhoneRepository 专用锁方法(findByIdForUpdate/findByIdsForUpdate) + allocate(完整校验链：状态=idle→分机号唯一性→号池归属→悲观锁→分配→写历史→通知) + reclaim(双轨回收：auto 回 idlestatus/ manual 归还号池 + allocation_org_id 归还 + 写历史 + 通知) |
| **前端** | AllocateDialog(选号码+选员工+分机号自动判定) + ReclaimDialog + 按钮可见性(Admin scope 内 + Ops 全局) |
| **验收** | 并发分配不会重复，分机号双轨判定正确，历史记录完整 |

> 此模块是系统最核心的并发瓶颈。严格 `@Lock(PESSIMISTIC_WRITE)` + `@Transactional(READ_COMMITTED)` + `version` 二道防线。

---

### M10 — 号码状态变更

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M09 |
| **后端** | reserve/release(仅 idle 可设置) + disable/enable(仅 idle 可设置) + trouble(active↔stopped) + surrender(二次确认 → cancelled + 写 surrender_record + re-entry 检测) |
| **前端** | ReserveDialog + DisableDialog + TroubleDialog(停机/复机) + SurrenderDialog(二次确认弹窗) + 按钮可见性(按状态+角色) |
| **验收** | 状态迁移图所有路径合法，违规操作被拒绝，二次入库检测生效 |

---

### M11 — 号码变更

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M09 |
| **后端** | change-user(分机号按新员工重新判定 auto/manual + 写 history) + change-number(单 @Transactional + 双号码 findByIdsForUpdate + ORDER BY id ASC 防死锁) + change-org(转移组织 + allocation_org_id 更新) + EMP-04 离职自动 reclaim(@Transactional 事务联动：员工→inactive + 号码回收 + 话机回收独立事务) + EmployeeNoValidator + PhoneNumberGenerator |
| **前端** | ChangeUserDialog + ChangeNumberDialog(选择新号码+分机号预判) + ChangeOrgDialog(选目标组织) + 离职确认弹窗 |
| **验收** | change-number 不会死锁，双号码操作原子性，离职联动回收正确 |

> change-number 是最复杂的并发操作：单事务 + 双号码一次锁 + ORDER BY ASC。必须写单元测试覆盖并发场景。

---

### M12 — 号码导入

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M02 |
| **新建表** | `import_batch` (batch_id + file_name + total_count + success_count + fail_count + status + error_detail JSON) |
| **后端** | ImportBatch entity + ImportService(异步 @Async + 批量查询 HashMap+IN + 归一化 + 判重 + surrender_record 检测) + 批量 INSERT(batchSize=100) + 导入进度轮询 API |
| **前端** | ImportPage(上传→进度条轮询→预览→确认) + 错误明细展示 |
| **验收** | 批量 100 条不超时，列优先匹配正确，失败行标记准确 |

---

### M13 — 话机基础

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M04 |
| **新建表** | `phone_device` (mac_address + model + status ENUM + org_id FK) + `device_phone_mapping` (M:N 关联) + `phone_device_history` (操作历史) |
| **后端** | Device entity + DeviceRepository + DeviceService(CRUD + MAC 归一化：去冒号+转大写+12位十六进制 + 状态变更 + 写 history) + DeviceController(16端点) |
| **前端** | DeviceTable(ProTable + 状态过滤 + 组织过滤) + DeviceDetail(信息+绑定号码+历史) + DeviceForm(录入/编辑) |
| **验收** | MAC 归一化正确，列表筛选正常工作，历史记录完整 |

---

### M14 — 话机操作

| 维度 | 内容 |
|------|------|
| **依赖** | M13 + M06 |
| **后端** | 分配/回收(scope 控制) + 停用/启用 + 送修(自动解绑) + 修复(回 active) + 报废(终态) + 绑定/解绑号码(按分机号，跨员跨组织允许，无分机号不可绑定) + 离职自动回收独立事务 + 操作通知 |
| **前端** | AllocateDeviceDialog + RecycleDeviceDialog + TroubleDialog + RepairDialog + RetireDialog + BindDialog + UnbindDialog + 按钮可见性(5状态矩阵) |
| **验收** | 送修自动解绑，绑定按分机号，跨组织绑定允许，离职回收联动 |

---

### M15 — 通知系统

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M09-M14 |
| **新建表** | `sys_notification` (type + title + content + target_user + related_id + is_read) |
| **后端** | Notification entity + NotificationService(统一发送入口) + 通知规则：直属部门 Admin(无则向上查找) + 所有 Ops + 号码操作/话机操作/号池预警触发 |
| **前端** | NotificationList(列表 + 已读/未读 + 点击跳转关联业务) + 顶部通知图标(Badge) |
| **验收** | 操作触发通知正确推送到目标角色，未读标记准确 |

---

### M16 — 权限体系收尾

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02-M15 |
| **后端** | 全局 @PreAuthorize(按角色+操作) + PermissionEvaluator(scope 裁剪：Admin 只能操作本部门及下级) + Boss/Finance 全系统只读拦截 |
| **前端** | 按钮可见性(按角色+scope 控制 show/hide) + 组织树：完整树展示 + scope 外置灰 + 可点击只读 |
| **验收** | Admin 无法操作 scope 外数据，Boss/Finance 无法执行写操作，组织树置灰+只读正确 |

---

### M17 — 仪表盘 & 功能开关

| 维度 | 内容 |
|------|------|
| **依赖** | M02-M16 |
| **新建表** | `sys_feature_flag` (feature_key + enabled + description) |
| **后端** | DashboardService(号码统计/组织统计/操作概览 API) + FeatureFlag entity + FeatureFlagService + @ConditionalOnFeatureFlag 注解 |
| **前端** | Dashboard 页面(统计卡片 + 图表) + 功能开关管理页 |
| **验收** | 仪表盘数据与实际一致，功能开关可实时控制模块可见性 |

---

## Phase 2：工单系统（M18-M22）

### M18 — 工单基础

| 维度 | 内容 |
|------|------|
| **依赖** | Phase1 完成 |
| **新建表** | `work_order` (work_order_no UNIQUE + type ENUM + status ENUM + priority + requester + handler + batch_id) + `work_order_item` (phone/device/employee 快照 + action + from/to 字段) |
| **后端** | WorkOrder entity + WorkOrderItem entity + repository + WorkOrderService(CRUD + 编号生成) + WorkOrderController |
| **前端** | WorkOrderTable(ProTable + 类型/状态过滤) + WorkOrderDetail(主表信息 + 明细列表) + WorkOrderForm |
| **验收** | CRUD 正常，工单编号唯一，快照字段记录正确 |

---

### M19 — 工单流转 & 批量拆单

| 维度 | 内容 |
|------|------|
| **依赖** | M18 |
| **后端** | 状态流转：pending→accepted→processing→completed→archived + 批量工单(跨部门自动拆分：按 org_id 分组 → 每组生成一条子工单 + 共享 batch_id) + 系统消息推送(处理进度→相关 Admin + Ops) |
| **前端** | 批量创建页面(多选号码 → 预览拆分结果 → 提交) + 处理页面(运维 accept/complete) + 归档页面(行政 archive) |
| **验收** | 流转正常无跳跃，批量跨部门正确拆分，消息推送达标 |

---

### M20 — 号码操作迁移工单驱动

| 维度 | 内容 |
|------|------|
| **依赖** | M18 + M09 + M10 + M11 |
| **后端** | 7 类工单(分配/过户/换号/转移/回收/拆机/停复机) + 状态变更仅通过工单触发(禁止直接调用 Phase1 的操作方法) + 操作前校验(工单状态+pending→操作结果写 work_order_item) + phone_history.work_order_no 关联 |
| **前端** | 调整现有操作页面：操作按钮→创建工单(预填信息)→提交→工单列表追踪 |
| **验收** | 所有号码操作只能通过工单，状态变更可追溯到工单 |

---

### M21 — 月度快照

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M02 |
| **新建表** | `phone_snapshot` (snapshot_month + phone_id UK + status + org_id/org_name 快照 + cost_center_code 快照 + is_surrendered + is_allocatable) |
| **后端** | PhoneSnapshot entity + repository + SnapshotService(快照生成：每月末自动执行 → 查询所有 active/stopped/cancelled 号码 → 写入快照 + 匹配成本中心) + 定时任务(@Scheduled + 失败重试 3 次) + 手动触发 API |
| **验收** | 月末自动生成快照，成本中心匹配正确，失败重试生效 |

---

### M22 — 基础报表

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M18 + M21 |
| **后端** | 号码资产报表(按组织/状态聚合 count + 导出 Excel) + 工单处理统计(按类型/时间/处理人聚合 + 平均处理时长) |
| **前端** | ReportPage(筛选条件 + 数据表格 + 导出按钮) + 简单图表(ECharts 柱状图/饼图) |
| **验收** | 统计数据与实际一致，Excel 导出格式正确 |

---

## Phase 3：费用 & 发票（M23-M27）

### M23 — 账单导入 & 分摊

| 维度 | 内容 |
|------|------|
| **依赖** | Phase2 完成 + M05 |
| **新建表** | `bill_raw` (bill_month + phone_number + charge_amount + raw_data JSON + import_status) + `bill_allocation` (关联 bill_raw + snapshot_org + cost_center + anomaly_flag + 确认字段) |
| **后端** | BillRaw entity(Excel 解析→列名映射→存 raw_data) + BillAllocation entity + AllocationService(自动匹配：phone_snapshot 按月份+号码匹配→归属部门+成本中心) + 多角色确认流程(行政确认归属+金额 → 财务确认异常 → 财务最终提交) |
| **前端** | 账单导入页(上传 Excel + 选择月份 → 解析预览) + 分摊确认页(行政：逐条确认归属+金额 + 标记异常) + 财务确认页(确认异常标记 + 最终提交) |
| **验收** | Excel 解析兼容常见模板，分摊匹配成本中心正确，多角色确认流转正常 |

---

### M24 — 异常检测

| 维度 | 内容 |
|------|------|
| **依赖** | M23 |
| **后端** | 历史金额对比(本月 vs 上月同号码金额) + 差异超阈值(可配置，默认 30%)→自动标记 anomaly_flag=1 + 异常原因写入 + 异常账单查询 API |
| **前端** | 异常账单列表(ProTable + 状态过滤 + 金额红色高亮) |
| **验收** | 阈值可配置，异常标记准确，财务报表可导出 |

> 小模块，半天开发量。与 M25 并行开发无冲突。

---

### M25 — 发票上传 & OCR

| 维度 | 内容 |
|------|------|
| **依赖** | Phase2 完成 + M02 |
| **新建表** | `invoice` (invoice_no UNIQUE + source_org + recipient_org + amount + status + ocr_text + ocr_confidence) + `invoice_file` (file_name + file_path + file_size + md5) |
| **后端** | Invoice entity + InvoiceFile entity + InvoiceService(上传→存文件→OCR 识别公司名称→匹配 org_structure.name→自动或待人工) + OCR 集成(Tesseract 或云服务 API) |
| **前端** | 发票上传页(批量 PDF + 命名校验) + OCR 结果展示(识别文本 + 匹配结果 + 置信度) + 手动匹配(待人工确认列表) |
| **验收** | PDF 上传正常，OCR 识别准确率≥80%（人工辅助兜底），匹配失败标记待确认 |

---

### M26 — 发票分发 & 确认

| 维度 | 内容 |
|------|------|
| **依赖** | M25 |
| **新建表** | `invoice_distribution` (invoice_id + recipient_user + status) + `subsidiary_reconciliation` (bill_month + subsidiary_org + total_amount + invoice_count + reconciliation_status) |
| **后端** | DistributionService(自动分发：匹配成功→已分发→通知子公司财务 + 匹配失败→待人工确认→通知集团财务) + ReconciliationService(子公司对账记录 + 线下确认流程：导出汇总→签字上传→集团确认) + InvoiceController(确认/查看/统计) |
| **前端** | 分发管理页(集团财务：待分发 + 已分发列表) + 发票确认页(子公司财务：查看→确认) + 对账页(导出汇总 + 上传签字凭证 + 确认状态) |
| **验收** | 自动分发率≥90%，子公司确认记录时间+操作人，对账状态流转正确 |

---

### M27 — 完整报表

| 维度 | 内容 |
|------|------|
| **依赖** | M22 + M23 + M24 + M26 |
| **后端** | 月度分摊报表(按部门/成本中心汇总费用 + 关联 bill_allocation 明细) + 发票收发统计(按子公司统计数量/金额 + 分发/确认状态) + 异常账单报表(异常标记明细 + 确认状态) + 所有报表支持导出 Excel |
| **前端** | 统一 ReportCenter 页面(多 Tab：号码资产/分摊/发票/异常/工单) + 通用筛选组件 + 图表可视化 + 导出按钮 |
| **验收** | 5 类报表数据与实际一致，筛选联动正确，Excel 导出完整 |

---

## 对比：旧版 Step vs 新版 Module

| 旧版 Step | 新版 Module | 变化 |
|-----------|------------|------|
| Step1 骨架(7任务) | M01 项目骨架 | 拆分更细：去掉 DDL 表创建(移到各模块) + 去掉种子数据(移到 M03/M04) |
| Step2 认证(6任务) | M03 员工 → M04 认证 | **顺序调整**：先建员工表再建用户表(FK 约束) |
| Step3 组织+员工(13任务) | M02 组织 + M03 员工 | 拆成 2 个独立模块 |
| Step4 号码查询(7任务) | M06 号码基础 | 一致 |
| Step5 号码操作(15任务) | M07+M08+M09+M10+M11+M12(6模块) | **大幅拆分**：池/区号/分配/变更/导入各自独立 |
| Step8 话机(12任务) | M13+M14(2模块) | 基础+操作分离 |
| Step7/M16 | M15+M16+M17(3模块) | 通知/权限/仪表盘分离 |
| Phase2(10d) | M18+M19+M20+M21+M22(5模块) | 工单基础→流转→迁移→快照→报表，逐层递进 |
| Phase3(10d) | M23+M24+M25+M26+M27(5模块) | 账单→检测→发票→分发→报表，逐层递进 |

---

## 每个模块的标准交付物

```
module/
├── Flyway 迁移: V{XX}__{module_name}.sql       ← 仅本模块需要的表
├── src/main/java/module/{name}/
│   ├── entity/{Name}.java                        ← JPA Entity
│   ├── repository/{Name}Repository.java           ← Spring Data JPA
│   ├── service/{Name}Service.java                 ← 业务逻辑
│   ├── dto/{Name}DTO.java                         ← 入参/出参
│   └── controller/{Name}Controller.java           ← REST API
├── src/main/frontend/src/pages/{name}/
│   ├── index.tsx                                  ← 页面组件
│   └── components/                                ← 子组件
└── 种子数据(如有): data/seed_{name}.sql
```

---

## 开发执行规则

1. **严格顺序**：M01 → M02 → M03 → M04 → 之后 M05-M17 可部分并行(取决于依赖)
2. **每模块自测**：完成后必须跑模块对应 API，不依赖后续模块
3. **表只建一次**：Flyway 迁移版本号严格递增，禁止回滚式修改
4. **并发模块先写测试**：M09/M11 必须先写并发单元测试再写业务代码
5. **前端启动即用**：每个模块前端页面可独立访问(通过路由注册)
6. **Gate Check**：每 3 个模块做一次集成回归，确保向后兼容

---

*本文件替代旧版 TASKS.md 作为开发顺序的唯一参考。*
*旧版 TASKS.md 中的具体任务细节仍有效，但执行顺序以此文件为准。*
