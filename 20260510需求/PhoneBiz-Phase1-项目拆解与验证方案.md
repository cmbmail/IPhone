# PhoneBiz Phase 1 — 项目拆解与风险验证方案

> 版本：1.0 | 日期：2026-05-10 | 基于需求清单 v5.0（终版）

---

## 一、分步构建计划（7 步）

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Step 1  │───→│ Step 2   │───→│ Step 3   │───→│ Step 4   │
│ 骨架    │    │ 认证鉴权  │    │ 组织员工  │    │ 号码查询  │
│ 2天     │    │ 2天      │    │ 3天      │    │ 2天      │
└─────────┘    └──────────┘    └──────────┘    └──────────┘
                                                    │
                                                    ▼
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Step 7  │←───│ Step 6   │←───│ Step 5   │    │ Step 5   │
│ 收尾部署 │    │ 号池导入  │    │ 号码操作  │    │ (续)     │
│ 1天     │    │ 2天      │    │ 3天      │    │          │
└─────────┘    └──────────┘    └──────────┘    └──────────┘
```

> 预估：**15 个工作日**（不含 QA 测试）。每步内部可以再分组并行。

---

### Step 1：项目骨架（2天）

**目标**：两端可运行、DB 可连接、统一规范就绪

| 后端 | 前端 |
|------|------|
| Gradle 项目 + Spring Boot 3.2 + 全部依赖 | Vite + React 18 + Antd + Router + axios + zustand + react-query |
| application.yml（MySQL连接） | vite.config.ts（proxy /api→8080） |
| 执行 DDL（16张表） | api/client.ts（axios实例+拦截器） |
| ApiResponse / PageResult / ResultCode / BaseEntity | types/index.ts（全量TS类型） |
| BusinessException / ErrorCode（20个） / GlobalExceptionHandler | AppLayout + Sidebar + Header 骨架 |
| DataInitializer（seed: admin/ops/boss + 待分配org） | LoginPage 骨架 + authStore 骨架 |

**验证**：`./gradlew bootRun` + `npm run dev` → 浏览器看到登录页 → 输入 admin/Phonebiz@2026 能调通 /api/auth/login（虽然还没实现）→ 返回 404 而非连接错误。

---

### Step 2：认证鉴权（2天）

**目标**：完整登录闭环、JWT 签发与校验、角色菜单

| 后端 | 前端 |
|------|------|
| JwtTokenProvider（生成/解析/过期） | LoginForm（username+password） |
| SecurityConfig + CorsConfig + JwtAuthenticationFilter | authStore（zustand: token+user+login+logout） |
| UserDetailsServiceImpl（查sys_user加载权限） | api/auth（login/logout/me） |
| AuthController + AuthService（login/logout/me） | 路由守卫（token过期→跳转login） |
| SysUserRepository + SysUser entity | Sidebar菜单按role显示 |
| 登录锁定逻辑（5次/30分钟 + 手动解锁API） | 首次登录强制改密页 |

**验证**：
1. `POST /api/auth/login` → 200 + Token
2. 错误密码 → 401
3. 5次失败 → 锁定（再次登录提示 ACCOUNT_LOCKED + 剩余分钟数）
4. Token 放 Authorization header → 访问受保护接口成功
5. 前端刷新页面 → Token 仍在 → 不跳转登录
6. 首次登录 → 强制跳改密页

---

### Step 3：组织架构 + 员工（3天）

**目标**：完整组织树 + 员工 CRUD + scope 裁剪 + 工号规则

| 后端 | 前端 |
|------|------|
| OrgStructure entity + repository + service + controller | OrgTree（Antd Tree，全局展示+scope外置灰） |
| TreeBuilder（平铺→树） | OrgFormDialog（新增/编辑/删除校验） |
| PermissionEvaluator（scope path LIKE） | EmployeeTable（分页+搜索+组织过滤） |
| Employee entity + repository + service + controller | EmployeeFormDialog（工号校验+字段校验） |
| EmployeeNoValidator + PhoneNumberGenerator | 员工列表显示当前号码+状态 |
| Employee创建时自动建 SysUser | — |

**验证**：
1. 组织树：admin 登录 → scope=集团 → 完整树可编辑；admin 登录 → scope=子公司A → 完整树但集团/子公司B置灰、可点击只读
2. 新增组织 → path自动计算正确（`/1/5/` 格式）
3. 同父节点下同名组织 → 拒绝 ORG_NAME_DUPLICATE
4. 有员工的部门 → 拒绝删除 ORG_HAS_EMPLOYEES
5. 员工列表 → 分页、搜索"张"、按组织过滤、按状态过滤 均正常
6. 工号 001234 → 自动计算分机号 1234；工号 ABC123 → 提示需号池（Step5 验证）

---

### Step 4：号码查询（2天）

**目标**：号码列表/详情/已拆机列表 + 历史时间线

| 后端 | 前端 |
|------|------|
| PhoneNumber entity + repository（含version） | PhoneTable（分页+6状态过滤+搜索） |
| PhoneHistory + PhoneSurrenderRecord entity + repository | PhoneDetail（属性+历史时间线） |
| PhoneService（list/get/history） | PhoneStatusBadge（6种颜色标签） |
| PhoneController（GET 端点） | 已拆机列表页 |

**验证**：
1. 号码列表 → 6种状态过滤各自正确
2. 按组织过滤 → 仅显示该组织和子组织的号码
3. 按号码搜索 → 模糊匹配
4. 号码详情 → 显示历史时间线（按时间倒序）
5. Admin scope外号码 → 不显示（数据层裁剪）

---

### Step 5：号码操作（3天）

**目标**：7种状态变更操作 + 预留/禁用 + 通知 + 并发锁

| 后端 | 前端 |
|------|------|
| PhoneService.allocate（完整校验链+FOR UPDATE） | PhoneAllocateDialog（选员工+分机号） |
| PhoneService.reclaim（双轨回收） | PhoneReclaimDialog |
| PhoneService.trouble（停机/复机） | PhoneTroubleDialog |
| PhoneService.surrender（归档+不可逆提示） | PhoneSurrenderDialog（二次确认） |
| PhoneService.changeUser/changeNumber/changeOrg | PhoneChangeUserDialog / PhoneChangeNumberDialog / PhoneChangeOrgDialog |
| PhoneService.reserve/release（预留）+ disable/enable（禁用） | PhoneReserveDialog / PhoneDisableDialog |
| NotificationService（号码操作→通知直属Admin+Ops；无Admin则向上查找） | 按钮可见性按状态规则 |
| 离职自动回收（EMP-04联动） | — |

**验证**：
1. allocate：idle→active；员工唯一号码校验；`extension_type='auto'`；FOR UPDATE 锁生效
2. reclaim：auto ext 清空（员工下次复用）；manual ext 清空+归还 allocation_org_id
3. 停机→复机，复机→停机：状态回环正常
4. surrender：二次确认弹窗 → cancelled → phone_history + surrender_record 双写
5. 预留：idle→reserved；解除预留：reserved→idle；reserved 状态无分配按钮
6. 禁用：idle→disabled；解除禁用：disabled→idle
7. 换号：两个号码事务一致（旧→idle + 新→active）；分机号按员工重新判定
8. 过户：新员工无号码校验；分机号按新员工重新判定
9. **并发测试**：两个线程同时 allocate 同一号码 → 一个成功一个返回 PHONE_NOT_IDLE

---

### Step 6：号池 + 区号匹配 + Excel 导入（2天）

**目标**：分机号池管理、区号匹配、批量导入

| 后端 | 前端 |
|------|------|
| ExtensionPool CRUD + 重叠检测 + 随机分配（≤100次） | ExtensionPoolTable + FormDialog |
| AreaCodeOrgMapping CRUD | AreaCodeMappingTable |
| Excel 导入服务（解析+归一化+区号/组织匹配+判重+surrender检测+预览+确认） | 导入页面（上传→预览表格→确认/重分配→提交） |

**验证**：
1. 号池范围重叠 → EP-02 拒绝 EXT_POOL_OVERLAP
2. 号池内分机号全被占用 → allocate 报 EXT_POOL_EXHAUSTED
3. 含字母工号员工所在部门无号池 → allocate 报 EXT_POOL_REQUIRED
4. Excel 导入：
   - 导入 100 行 → 预览表格显示成功/待分配/重复
   - Excel列有值 → 精确匹配组织名
   - Excel列为空 → 走区号匹配
   - 号码已在 surrender_record → is_reentry=1
   - 待分配行 → 批量重分配组织 → 再次提交成功
5. 手机号/400号码无区号 → 标记"无法识别"

---

### Step 7：Dashboard + 权限收尾 + 部署（1天）

**目标**：仪表盘、全局权限贯通、可部署

| 后端 | 前端 |
|------|------|
| Dashboard统计 API（scope/全局号码状态统计） | Dashboard页面（角色+快捷入口+统计卡片） |
| 全局 @PreAuthorize 审计 | 前端按钮按角色+状态双重控制 |
| docker-compose.yml | API文档 Swagger UI |
| — | 通知消息列表（未读数+列表+标已读） |
| — | 全流程端到端测试 |

**验证**：
1. Admin 登录 → Dashboard 仅显示 scope 内统计
2. Boss/Finance → 所有页面无编辑按钮
3. Finance 直接调用 POST /api/phones/allocate → 403 OUT_OF_SCOPE
4. `docker-compose up` → 完整可运行

---

## 二、风险清单

### 🔴 高风险（可能导致返工或系统不可用）

| # | 风险 | 类别 | 影响 | 发生条件 |
|---|------|------|------|---------|
| **R1** | **并发分配冲突**：两 Admin 同时 allocate 同一号码 → 一人拿到，另一人报错但号码已被改 | 并发 | 数据不一致 | FOR UPDATE 未正确覆盖；或事务隔离级别过低 |
| **R2** | **change-number 事务断裂**：旧号码变 idle、新号码未变 active → 员工失去号码 + 两个号码状态混乱 | 事务 | 数据损坏 | @Transactional 未覆盖两条 UPDATE；数据库中途宕机 |
| **R3** | **分机号池耗尽无提示**：部门 30 人含字母工号，号池仅 10 个 → 20 人无法分配 | 资源 | 业务阻塞 | 号池范围配置过小；管理员未及时发现 |
| **R4** | **组织 path 超长**：深度 50+ 层的组织树 → path VARCHAR(500) 溢出 → INSERT 失败 | 边界 | 功能不可用 | 组织层级创建未提前拦截 |
| **R5** | **Excel 导入大批量超时**：500 行 × 逐行 DB 查询（判重+组织匹配+surrender检测）→ HTTP 超时 | 性能 | 导入失败 | 同步处理 + 数据库未建索引 |

### 🟡 中风险（影响体验或需要补救）

| # | 风险 | 类别 | 影响 |
|---|------|------|------|
| **R6** | **离职自动回收失败**：员工有 active 号码但 reclaim 时号池问题导致回滚 → 离职操作整体失败 | 事务 | 员工无法离职 |
| **R7** | **通知风暴**：50 个 Ops + 每个号码操作推送 → sys_notification 表膨胀 + 前端消息列表拉取慢 | 资源 | 性能下降 |
| **R8** | **号池随机分配性能**：号池 100000-999999（90万个），随机选号可能大量命中已用号码 → 100 次重试可能不够 | 性能 | 分配超时 |
| **R9** | **虚拟员工被误操作**：有人编辑/删除虚拟员工 → 公共号码失去归属 | 生命周期 | 数据错乱 |
| **R10** | **区号匹配表配置不全**：未配置区号映射 → 大量导入进"待分配" → 人工分配工作量大 | 资源 | 导入效率低 |
| **R11** | **Token 有效期内的权限变更**：Admin 被降权 → 已签发的 24h Token 仍可操作 scope 外数据 | 安全 | 越权操作 |
| **R12** | **auto 分机号与 manual 分机号隐形冲突**：工号 000123→分机号 123。同时有人工分机号 000123（以0开头被拒）或 123（3位vs6位格式不同不冲突） | 边界 | 暂时安全但需关注 |

### 🟢 低风险（可接受或容易规避）

| # | 风险 | 类别 |
|---|------|------|
| R13 | 分页 page/size 边界值导致空结果 | 边界 |
| R14 | Excel 列名不一致（用户忘改模板） | 使用 |
| R15 | 手机号格式多样（+86-10-12345678）无法归一化 | 边界 |
| R16 | Boss 账号被禁用 → 无人可手动解锁其他账号 | 生命周期 |

---

## 三、验证方案

### 3.1 核心流程验证（6 条 Happy Path）

| # | 验证场景 | 操作步骤 | 预期结果 | 验证方式 |
|---|---------|---------|---------|---------|
| V1 | 号码全生命周期 | idle→allocate→trouble停机→trouble复机→reclaim→surrender | 每次状态转移正确；history 6条记录 | 手动端到端 + API 断言 |
| V2 | 跨组织分配 | 子公司A号码(idle) → 分配给子公司B员工 | 成功；号码org不变；员工持有号码 | 手动 |
| V3 | Excel 导入带待分配 | 导入 50 行含 5 行无匹配组织 → 预览 → 批量重分配 → 提交 | 最终 50 行全部入池；5行org更新 | 手动 + 数据库校验 |
| V4 | 离职自动回收 | 员工E(active号码) → 标记离职 | 员工 inactive；号码 idle；phone_history 1条 reclaim | API 断言 + 事务验证 |
| V5 | 分机号双轨切换 | 号码A(auto ext) → reclaim → 分给含字母员工 → 自动变 manual | extension_type 从 auto→null→manual；分机号变化 | API 断言 |
| V6 | 二次入库 | surrender 号码 → 重新逐条录入 | is_reentry=1；phone_number 恢复 idle | 数据库校验 |

### 3.2 并发验证（3 条）

| # | 验证场景 | 方法 | 预期 |
|---|---------|------|------|
| V7 | **双 Admin 同时 allocate 同一号码** | JMeter 2 线程；同一 phone_id；并发执行 POST /allocate | 1 个 200，1 个 400（PHONE_NOT_IDLE）；无脏写 |
| V8 | **change-number 并发** | Admin A 换号(1→2)；Admin B 同时 allocate 号码2 | A 成功(200)，B 报错；或 B 成功 A 失败；不会两个都成功 |
| V9 | **surrender + allocate 并发** | Admin A surrender(1)；Admin B allocate(1) | A 成功→号码 cancelled；B 报 PHONE_NOT_IDLE |

### 3.3 边界验证（5 条）

| # | 验证场景 | 方法 | 预期 |
|---|---------|------|------|
| V10 | 工号 000001→分机号 1 | allocate；检查 extension_number | extension_number='1'；extension_type='auto' |
| V11 | 号池 start=end | POST /api/ext-pools；start=100000,end=100000 | 400 拒绝 |
| V12 | 同父节点同名组织 | POST /api/orgs；同父下 name="测试部"已存在 | 400 ORG_NAME_DUPLICATE |
| V13 | org path 超 VARCHAR(500) | 循环创建子组织 60 层 | 最后一步 400 ORG_DEPTH_EXCEEDED |
| V14 | page=0 / size=-1 | GET /api/phones?page=0&size=-1 | 200；默认 page=1,size=20 |

### 3.4 性能验证（3 条）

| # | 验证场景 | 方法 | 阈值 |
|---|---------|------|------|
| V15 | 号码列表 10000 条+分页 | JMeter 20并发 GET /api/phones | P95 < 500ms |
| V16 | 组织树 200 节点（深度10） | GET /api/orgs/tree | P95 < 300ms（含统计计算） |
| V17 | Excel 导入 500 行 | 提交导入 → 等待异步完成 | 总耗时 < 30s；无 OOM |

### 3.5 权限验证（4 条）

| # | 验证场景 | 方法 | 预期 |
|---|---------|------|------|
| V18 | Finance 调用 allocate | Finance Token → POST /api/phones/1/allocate | 403 |
| V19 | Admin scope外号码查看 | Admin(scope=子公司A) → GET /api/phones?orgId=子公司B | data.list 为空 |
| V20 | Boss 调用 delete org | Boss Token → DELETE /api/orgs/1 | 403 |
| V21 | 禁用用户访问 | 禁用 admin → admin 用已有 Token 访问 | 200（Token未过期，不维护黑名单） |

---

## 四、关键验证门禁（准入标准）

每步完成后必须通过以下检查才能进入下一步：

| Step | 门禁检查 |
|------|---------|
| Step 1 | `bootRun` 启动成功 + `npm run dev` 无编译错误 + MySQL 表全部创建 + seed 数据写入 |
| Step 2 | 登录 200 + 错误密码 401 + 锁定机制生效 + 前端路由守卫 |
| Step 3 | 组织树正确（scope裁剪）+ 员工CRUD + 同名拒绝 |
| Step 4 | 号码列表分页/过滤/搜索 + 详情历史时间线 |
| Step 5 | **全部 7 种操作** + 预留/禁用 + V7-V9 并发测试通过 |
| Step 6 | 号池重叠拒绝 + Excel 导入（含待分配流）+ 区号匹配 |
| Step 7 | Dashboard 统计正确 + 权限全覆盖 + V18-V21 通过 |

---

## 五、最可能出问题的 5 个点（⚠️ 重点关注）

| # | 问题 | 为什么容易出问题 | 防御措施 |
|---|------|----------------|---------|
| 1 | **FOR UPDATE 没锁住** | JPA 默认不生成 FOR UPDATE；需要 @Lock 或 @Query 显式声明 | 并发测试 V7-V9 强制验证；代码 Review 确认每个操作都有锁 |
| 2 | **分机号双轨逻辑混乱** | auto/manual 在 alloc/reclaim/change-user/change-number 四种场景下行为各不相同 | 写单元测试覆盖 4×2=8 种组合 |
| 3 | **Excel 导入归一化判重不准确** | "010-12345678" vs "01012345678" vs "010 1234 5678" 归一化后相同但用户意图可能不同 | 导入前预览表格显示判重结果，让 Ops 确认 |
| 4 | **离职回收事务过大** | 一个员工可能持有多个号码（虽然规则不允许，但数据可能异常）→ 回收失败导致离职操作回滚 | 回收前检查号码数量；逐个回收而非批量；回收失败不影响离职（标记为待处理） |
| 5 | **号池随机选号性能** | 号池范围大（900K）+ 大量已用 → 随机碰撞命中率低 | 不用纯随机，改用预计算「可用列表」或改用顺序查找第一个未用 |

---

*本文档供开发团队在实现过程中对照使用。每步完成后的门禁检查必须通过才能推进。*
