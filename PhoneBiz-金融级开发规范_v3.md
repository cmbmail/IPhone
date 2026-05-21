---
AIGC:
  ContentProducer: '001191110102MAD55U9H0F10002'
  ContentPropagator: '001191110102MAD55U9H0F10002'
  Label: '1'
  ProduceID: '00d4ee7e-b5e7-43ef-afa1-b86f630204d6'
  PropagateID: '00d4ee7e-b5e7-43ef-afa1-b86f630204d6'
  ReservedCode1: '2a88fdb5-c507-409c-9d17-9b5be061baa3'
  ReservedCode2: '2a88fdb5-c507-409c-9d17-9b5be061baa3'
---

# PhoneBiz 金融级开发规范 v3

> 生成：2026-05-14 | 更新：2026-05-20 | 参考金融体系"四阶研发+双轨治理"模式，适配 PhoneBiz 企业内线电话管理平台
> 原则：**金融级严谨管控 + 27 模块按数据依赖自底向上 + 每个模块独立交付可验收**

---

## 目录

1. 研发体系总纲（§1.4 AI全栈开发约束：契约驱动+后端先行+6步流程）
2. 代码规范（强制）
3. 安全规范
4. 环境与工具链
5. 27 模块开发计划（增强版）
6. 质量门禁（模块级+集成级含C4新会话验证+Phase级+发布级）

---

## 1. 研发体系总纲

PhoneBiz 采用 **"四阶研发 + 双轨治理"** 的金融级流程。

### 1.1 研发四阶段

```
需求阶段 ──→ 开发阶段 ──→ 测试阶段 ──→ 运维阶段
    ↑                                        │
    └──────────── 定期复盘反馈 ───────────────┘
```

| 阶段 | 核心动作 | PhoneBiz 落地 |
|------|---------|--------------|
| **需求** | 业务需求→技术方案评审(架构/安全/性能) | 已完成：需求清单 v5.2 + 知识库 26 决策 + DDL 24 表 |
| **开发** | 分支开发→单元测试→代码评审(≥1人)→静态扫描→安全审计 | 每个 Module 独立分支 + 单元测试 ≥80% + PR 评审 + CheckStyle + OWASP |
| **测试** | 功能测试→集成测试→性能压测→安全测试 | 每 Module 自测；每 3 Module 集成回归；Phase 收尾全量压测 |
| **运维** | 发布→监控→应急响应→回滚→复盘 | Docker Compose 部署 + 日志监控 + DB 备份 + 回滚预案 |

### 1.2 双轨治理

| 轨 | 内容 | 门禁 |
|----|------|------|
| **代码轨** | 命名/格式/注释/异常/日志/输入校验 | pre-commit 钩子 + CheckStyle |
| **安全轨** | 接口鉴权/数据脱敏/审计日志/依赖扫描 | 每 Module 安全自查 + OWASP 扫描 |

### 1.3 架构适配

| 金融标准 | PhoneBiz 适配 |
|---------|--------------|
| 微服务+中台化 | **模块化单体**：`module.{auth,org,phone,device,workorder,bill,invoice}` 包隔离 |
| 高可用：读写分离/多活 | **暂不适用**：内部系统，单库 + 定时备份 |
| 技术栈 | React 18 + Spring Boot 3.2 + JPA + MySQL 8 + Flyway + Docker Compose |
| 环境隔离 | **三环境**：dev → test → prod，数据脱敏同步 |

### 1.4 AI 全栈开发约束（契约驱动，后端先行）

> 同一个 Module 内，AI 做前后端开发的核心风险是**两端独立正确、拼起来不对**。
> 以下 5 大约束消除这个风险。

#### 风险对照

| 风险 | 描述 | 约束 |
|------|------|------|
| R1 契约漂移 | 后端改字段名，前端还走旧名 → 运行时 500 | C1 契约先行 |
| R2 上下文断裂 | 长对话遗忘前面决策，各自为政 | C4 新会话验证 |
| R3 质量不均衡 | 后端严谨、前端凑合 | C2 后端先行（后端跑通才准开前端） |
| R4 错误级联 | 一端小错被误判→越修越错 | C2+C5 分步验证 |
| R5 验收真空 | 前后端都说"完成"，从未联调 | C3 同 PR 提交 + 集成门禁 |

#### 约束明细

| 约束 | 内容 | 落地方式 |
|------|------|---------|
| **C1 契约先行** | 每个 Module 第一步是写 DTO（Java）+ 对应 TS 类型，两端以 DTO 为唯一真源 | 计入 Module 交付步骤第 1 步 |
| **C2 后端先行** | DB → 后端 → curl 验证端点 → 后端跑通前**禁止写任何前端代码** | 计入 Module 交付步骤第 3 步 |
| **C3 同 PR 提交** | 前后端 DTO 类型必须同一 PR，Review 时对照检查，禁止分两次合 | Git 分支规范 + PR Review |
| **C4 新会话验证** | 每 3 个 Module 后用新对话加载 KB+v3，执行回归测试 | 计入集成门禁 I4。**AI 开发时触发方式**：当 Module 序号是 3 的倍数时，输出提示「请在新对话中执行 `/phonebiz regression M01-M{xx}`」；回归在新对话中 curl 全集端点并检查 Flyway 版本连续性 → 全部通过后手动确认 → 继续下一个 Module |
| **C5 文档驱动** | 命名/异常/日志/安全规范不靠 AI 记忆，靠每次对话加载 v3；**版本不靠 AI 查最新，靠 `build.gradle` / `package.json` 为唯一真源** | Skill 启动流程 |

#### 每个 Module 的 6 步开发流程

```
Step 1 合同 → Step 2 数据库 → Step 3 后端 → Step 4 契约验证 → Step 5 前端 → Step 6 门禁
      ↑                                    ↑                      ↑
  定义 DTO + 端点列表               curl 验证所有端点            对接已验证 API
  (后端/前端类型对齐)           ← 后端跑通前不写前端 →          ← 不盲调 →
```

| 步 | 内容 | 产出 | 验证方式 |
|----|------|------|---------|
| **Step 1 合同** | 定义 Java DTO（入参/出参）+ API 端点列表 + 前端 TS 类型 | `PhoneDTO.java` + `phone.ts` | 字段名、类型、枚举值前后端一致 |
| **Step 2 数据库** | Flyway 迁移文件 | `V{XX}__{module}.sql` | MySQL 执行成功，表结构正确 |
| **Step 3 后端** | Entity → Repository → Service → Controller | 可运行的 API | `gradle bootRun` + curl 每个端点 |
| **Step 4 契约验证** | curl/Postman 验证所有端点响应符合 DTO | 所有端点 200/201 + 错误码正确 | **后端跑通前不写前端** |
| **Step 5 前端** | 页面 → 组件 → 对接已验证 API → 联调 | 可交互页面 | 手动操作流程完整 |
| **Step 6 门禁** | G1-G5 全部通过 | 见 §6.1 | 🔴阻断项全部 ✅ |

#### 两种执行策略：按模块类型选择

上述 6 步在**简单 CRUD 模块**中线性执行即可。但在**复杂业务模块**中，Step 3-5 需要按**端点粒度循环**，以应对前端开发过程中对 API 的反馈调整。

| 策略 | 适用模块 | 方式 | 为什么 |
|------|---------|------|--------|
| **整模块线性** | M02/M03/M05/M07/M08/M17/M22/M24 | Step3 写完全部后端 → Step4 验证全端点 → Step5 写完全部前端 | 端点少（≤5），纯读写，不存在"前端发现缺端点" |
| **端点粒度循环** | M06/M09/M10/M11/M12/M13/M14/M18/M19/M20/M21/M23/M25/M26/M27 | Step3-5 循环：每完成 **1 个端点**的后端→curl→前端，再开始下一个 | 端点数量较多（>5），或含并发/状态机/多角色流程，前端大概率需要调整数据形态，小步快反馈 |

**端点粒度循环示意（以 M06 号码基础为例）**：

```
第1轮：GET /api/phone-numbers（列表+分页+筛选）
       → Entity + Repo + Service + Controller
       → curl 验证：{ code:200, data:{ content:[...], totalElements:47 } }
       → 前端 PhoneTable（对接 content + totalElements）
       → 发现需要 orgName → 回改后端 DTO 加字段 → curl 再验 → 前端更新 ✅
       → 此端点锁定，不再改动

第2轮：GET /api/phone-numbers/{id}（详情）
       → ... → curl → 前端 PhoneDetail ✅

第3轮：GET /api/phone-numbers/{id}/history（历史时间线）
       → ... → curl → 前端 Timeline 组件 ✅

第N轮：... 依此类推
```

**铁律**：每个端点的后端一旦 curl 验证通过，该端点锁定。前端需求变更如果需要新字段/新端点，只影响正在开发的当前端点，不波及已锁定的端点。

#### 端点粒度循环 vs 整模块线性的决策规则

```
问：此模块的端点数量？
  ≤5 → 整模块线性
  ≥5 → 问：此模块是否有并发逻辑或状态机？
          是 → 端点粒度循环（先写并发单元测试，再写业务代码）
          否 → 问：前端开发中是否可能发现需要新端点/新字段？
                  很可能 → 端点粒度循环
                  不太可能 → 整模块线性
```

---

## 2. 代码规范（强制）

> 所有代码（含 AI 辅助生成）统一遵循。pre-commit 不通过禁止提交。

### 2.1 命名规范

#### Java

| 元素 | 规则 | ✅ 示例 | ❌ 禁止 |
|------|------|---------|---------|
| 包名 | 全小写 | `com.phonebiz.module.org` | `com.phoneBiz` |
| 类名 | PascalCase | `OrgService`, `PhoneNumber` | `orgService` |
| 方法/变量 | camelCase | `findByOrgId()`, `employeeNo` | `FindByOrgId()`, `p_sts` |
| 常量 | UPPER_SNAKE_CASE | `MAX_DEPTH`, `DEFAULT_PAGE_SIZE` | `maxDepth` |
| 枚举值 | UPPER_SNAKE_CASE | `IDLE`, `ACTIVE`, `STOPPED` | `Idle` |
| DB 字段 | snake_case | `phone_number`, `created_at` | `phoneNumber` |

**禁止**：拼音命名、无意义缩写（`tmp`→`tempFile`）、单字母变量（循环 `i`/`j` 除外）

#### TypeScript/React

| 元素 | 规则 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `PhoneTable.tsx`, `OrgTree.tsx` |
| 函数/Hooks | camelCase / `use` 前缀 | `formatPhoneNumber()`, `useAuthStore()` |
| 类型/接口 | PascalCase | `PhoneDTO`, `OrgTreeNode` |
| 常量 | UPPER_SNAKE_CASE | `PHONE_STATUS_COLORS` |

### 2.2 格式规范

| 规则 | Java | TypeScript/React |
|------|------|-----------------|
| 行宽 | 120 | 120 |
| 缩进 | 4 空格（禁止 Tab） | 2 空格 |
| 文件末尾 | 一个空行 | 一个空行 |
| import 排序 | 按包路径字母序 | 第三方→项目内→相对路径 |

### 2.3 注释规范

**必须写**：
- 所有 public 类：Javadoc（`@author` + `@since` + 一句话描述）
- 所有 public 方法：Javadoc（`@param` + `@return` + `@throws`）
- 复杂业务逻辑：行内 `//` 解释 **WHY**，不是 WHAT
- 并发关键代码：行内 `//` 说明锁策略

**禁止写**：
- 冗余注释（`// 设置 name` 对着 `obj.setName()`）
- 注释掉的大段废弃代码（用 Git 追溯）
- 无日期/负责人的 `// TODO`

**示例**：

```java
/**
 * 号码分配服务。所有状态变更必须通过 {@link #findByIdForUpdate} 获取悲观锁。
 *
 * @author phonebiz-team
 * @since 1.0.0
 */
@Service
public class PhoneAllocateService {

    /**
     * 将号码分配给指定员工。分机号：纯数字工号走 auto 轨，含字母走 manual 轨。
     *
     * @param phoneId    号码 ID（必须 idle 状态）
     * @param employeeNo 员工工号（必须 active 状态）
     * @return 分配后的号码实体
     * @throws BusinessException 号码不可分配时抛出
     */
    @Transactional
    public PhoneNumber allocate(Long phoneId, String employeeNo) {
        // 悲观锁：防止并发重复分配
        PhoneNumber phone = phoneRepository.findByIdForUpdate(phoneId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
        // ...
    }
}
```

### 2.4 异常处理规范

| 规则 | 说明 |
|------|------|
| 强制捕获 | checked exception 必须显式捕获或声明 throws |
| 禁止空 catch | catch 块至少记录日志，禁止 `catch (Exception e) { }` |
| 含上下文 | 异常消息包含操作人 ID、号码、工单号等 |
| 统一异常类 | 使用 `BusinessException(ErrorCode)`，禁止裸 `RuntimeException` |
| 全局处理 | `@RestControllerAdvice` → `ApiResponse.error(code, message)` |
| 不暴露内部信息 | 生产环境异常不暴露 SQL、堆栈、IP |

**错误码格式**：`{模块}-{类型}{序号}`，如 `PHONE-001`（号码不存在）、`AUTH-002`（密码错误）

**错误码严重等级与追溯闭环**：

| 等级 | 范围 | 触发动作 |
|------|------|---------|
| Critical | AUTH-001~099（认证）、BILL-（账单）开头 | 立即告警 → 自动建单 → 4h 内响应 |
| Warning | PHONE-（号码操作）、WO-（工单）开头 | 告警 → 记录 → 次日处理 |
| Info | 其余 | 记录日志，月末汇总分析 |

**闭环流程**：`BusinessException` 抛出 → `GlobalExceptionHandler` 记录 ERROR 日志（含 errorCode + operator + context）→ ELK 日志系统根据 errorCode 关键词触发告警 → 值班人员确认 → 问题单关联 errorCode 和 git blame → 修复分支合并 → 验证时对照 errorCode 回归 → 关闭问题单。禁止：告警后不建单、建单后不响应、响应后不验证。

### 2.5 日志规范

| 规则 | 说明 |
|------|------|
| 统一框架 | SLF4J + Logback（禁止 `System.out`/`System.err`） |
| 分级 | DEBUG：开发调试 / INFO：业务流程关键节点 / WARN：可恢复异常 / ERROR：需人工介入 |
| **禁止打印** | 手机号、密码（含 hash）、Token、密钥、生产 SQL 参数 |
| 脱敏输出 | 手机号 `138****1234` |
| 变更必记 | create/update/delete 记录 INFO（操作人+对象ID+变更摘要） |
| 审计日志 | 拆机/账单确认/发票分发 → 独立审计日志表 |

```java
// ✅ 正确
log.info("号码分配成功 phoneId={} employeeNo={} type={}", phoneId, empNo, type);
log.warn("号池预警 orgId={} usage={}% threshold={}%", orgId, rate, threshold);

// ❌ 错误
log.info("号码 13912345678 分配给 张三");       // 暴露隐私
log.error("账单导入失败", exception);          // 无上下文 batchId
```

### 2.6 输入校验

| 层级 | 校验内容 | 工具 |
|------|---------|------|
| Controller | 非空/格式/长度/范围 | `@Valid` + `@NotNull/@NotBlank/@Size/@Pattern` |
| Service | 状态合法性/权限/业务规则 | 显式校验 → `BusinessException` |
| Repository | 防 SQL 注入 | JPA 参数化查询（禁止拼接 SQL） |
| 文件上传 | 类型(MIME)/大小/内容 | 后端校验，不信任前端 |

### 2.7 前端架构规范

#### 状态管理

| 场景 | 方案 | 说明 |
|------|------|------|
| 服务端数据（列表/详情） | **React Query（TanStack Query）** | 自动缓存、去重、后台刷新、分页 |
| 客户端状态（认证/UI） | **Zustand** | 轻量，`authStore` 已用 |
| 表单状态 | **Antd Form** 内置 | 不额外引入 Formik/React Hook Form |

**禁止**：每个模块自造状态管理方案。全局仅 2 个 Store：`authStore`（认证）+ `uiStore`（侧边栏折叠/主题）。

#### API 调用封装

```
src/api/
  client.ts          ← axios 实例（baseURL + 拦截器）
  modules/
    auth.ts          ← login/logout/me
    org.ts           ← 组织 CRUD
    phone.ts         ← 号码 CRUD + 操作
    ...
```

**规则**：
- 所有 API 函数返回 Promise，调用方用 React Query 的 `useQuery`/`useMutation` 包装
- 禁止在组件中直接 `axios.get/post`，必须通过 `api/modules/*.ts`
- 401 拦截器统一处理，业务代码不感知 Token 过期

#### 前端布局规范

**整体布局**：固定侧边栏 + 顶部栏 + 内容区三段式布局。

| 区域 | 说明 |
|------|------|
| 侧边栏 | 固定宽度 260px，收起宽度 72px，深色背景 |
| 顶部栏 | `position: fixed` 贴顶，包含页面标题、搜索框、通知、用户信息 |
| 内容区 | `padding-top` 补偿 fixed header 高度 |

**侧边栏菜单规范**：

| 规范项 | 说明 |
|--------|------|
| 分组方式 | SubMenu 可折叠分组（非固定展开 group），默认全部收起 |
| 菜单分组顺序 | 工单管理 → 号码资源（号码/分机池/区号/设备）→ 费用管理（成本中心/账单/分摊/发票/对账）→ 组织人员（组织架构/员工）→ 系统（报表中心） |
| 图标 | 每个菜单项使用独立图标，禁止多个菜单项共用同一图标 |
| Logo | 点击左上角 PhoneBiz Logo 返回系统看板（/dashboard） |
| 系统看板 | 无菜单项，登录后默认进入 /dashboard |
| 收起按钮 | 底部收起/展开按钮仅显示图标，不显示文字 |
| 色彩 | 一二级菜单默认颜色与侧边栏背景一致（无对比），仅鼠标 hover 时变色 |

**用户菜单交互规范**：

| 功能 | 交互方式 |
|------|---------|
| 修改密码 | 点击右上角用户头像 → 下拉菜单「修改密码」→ Modal 弹窗（当前密码+新密码+确认新密码，新密码≥8位） |
| 退出系统 | 下拉菜单「退出登录」→ Popconfirm 气泡二次确认（"确定要退出当前账号吗？" + 确定/取消）→ 清除登录态跳转登录页 |
| 首次登录 | 登录成功后提示"首次登录，请修改密码"，前端根据 `forceChangePassword` 标志判断 |

**搜索框规范**：
- 外层容器一个框，内部 input 无独立边框（transparent background + no border + no box-shadow）
- placeholder 文字：`搜索...`

#### 组件拆分规则

| 组件行数 | 处理 |
|:--:|------|
| ≤150 行 | 单文件，不拆分 |
| 150-300 行 | 抽子组件到 `./components/` |
| >300 行 | 必须拆分，否则 Review 不通过 |

**页面目录结构**：
```
src/pages/phone/
  index.tsx              ← 页面入口（路由注册、布局）
  components/
    PhoneTable.tsx        ← 列表核心
    PhoneDetail.tsx       ← 详情
    PhoneStatusBadge.tsx  ← 状态标签
    dialogs/
      AllocateDialog.tsx  ← 操作弹窗
```

#### 三态处理（强制）

每个数据展示组件必须处理三种状态：

| 状态 | 实现 | 示例 |
|------|------|------|
| **loading** | Antd `Skeleton` 或 `Spin` | 表格加载时显示骨架屏，不显示空表格 |
| **empty** | Antd `Empty` + 操作引导 | "暂无号码，点击导入" |
| **error** | Antd `Alert` + 重试按钮 | "加载失败，点击重试" 调 `refetch()` |

**禁止**：loading 时显示空白页、error 时静默失败（用户不知道出了什么问题）。

#### 路由注册规则

| 规则 | 说明 |
|------|------|
| 路由路径 | `/模块名`，如 `/phone`、`/org`、`/work-order` |
| 懒加载 | 所有页面组件 `React.lazy(() => import(...))` |
| 权限守卫 | 路由配置中声明 `requiredRole`，无权限不渲染 |
| 404 | 未匹配路由显示 404 页，不白屏 |

---

## 3. 安全规范

### 3.1 接口安全

| 措施 | 落地 |
|------|------|
| 认证 | JWT（无状态），30 分钟过期 |
| 传输 | HTTPS（生产强制） |
| 权限 | `@PreAuthorize` 按角色 + `PermissionEvaluator` 按 scope |
| 防重放 | JWT 含 `jti` 唯一 ID |
| 幂等 | 号码分配/工单创建做幂等 |
| CORS | 仅允许前端域名 |

### 3.2 数据安全

#### 分级

| 级别 | 内容 | 存储 | 访问 |
|------|------|------|------|
| 公开 | 组织名称、部门列表 | 明文 | 登录用户 |
| 内部 | 员工姓名、工号、分机号 | 明文 | scope 授权 |
| 机密 | 电话号码、账单金额、发票信息 | 明文 + 审计日志 | 角色最小权限 |
| 绝密 | 用户密码、JWT 密钥 | bcrypt 哈希 / 环境变量 | 认证模块内部 |

> PhoneBiz 不涉及身份证、银行卡号，无需国密 SM2/SM3/SM4。

#### 脱敏

| 场景 | 规则 |
|------|------|
| 开发/测试环境 | 电话号码 → `1380000{序号}`；姓名 → `测试用户{序号}` |
| 日志输出 | 电话号码 → `139****5678` |
| API 响应 | 不返回 `password_hash`，scope 外数据不返回 |

#### 留存

| 数据 | 留存 | 清理 |
|------|------|------|
| 号码操作历史 / 拆机归档 / 账单 / 分摊 / 发票 | 永久 | 不清理 |
| 系统通知 | 6 个月 | 定时清理 |
| 导入临时文件 | 3 个月 | 定时清理 |
| 系统日志 | 6 个月 | 按天归档 |

### 3.3 审计日志（独立于业务 history 表）

| 操作 | 必记字段 |
|------|---------|
| 登录/登出 | 用户名、IP、时间、结果 |
| 权限变更 | 操作人、目标用户、变更前后角色、时间 |
| 拆机确认 | 号码、操作人、工单号、二次确认结果 |
| 账单提交 | 月份、操作人、提交时间 |
| 发票分发 | 发票号、来源/目标公司、操作人 |

### 3.4 依赖安全

- 所有依赖使用明确版本号，**禁止 SNAPSHOT/LATEST**
- PR 合并前 `gradle dependencies` 扫描已知漏洞（OWASP Dependency Check）
- 高危漏洞（CVSS ≥ 7.0）必须修复或升级后方可合并

---

## 4. 环境与工具链

### 4.1 三环境

```
dev（开发） ──→ test（测试） ──→ prod（生产）
  实时数据       脱敏数据         真实数据
  热重载         全量功能         性能优化
```

| 环境 | 数据 | 部署 |
|------|------|------|
| dev | Mock + 种子数据 | 本地 `gradle bootRun` + `npm run dev` |
| test | 脱敏同步 | Docker Compose（MySQL + App + Nginx） |
| prod | 真实数据 | Docker Compose（volume 持久化 + 定时备份） |

### 4.2 工具链

> 所有版本精确锁定。AI 开发场景下版本漂移 = 隐式 API 断裂，不可接受。

| 环节 | 工具 | 版本 | 锁定级别 | 门禁 |
|------|------|------|:--:|------|
| Java 运行时 | OpenJDK | **17**（LTS） | 🔴 精确锁 | `java -version` |
| 后端框架 | Spring Boot | **3.2.5** | 🔴 精确锁 | `build.gradle` |
| 构建工具 | Gradle | **8.7** | 🔴 精确锁 | `gradle --version` |
| 数据库 | MySQL | **8.0.36** | 🟡 次版本锁 | `SELECT VERSION()` |
| DB 迁移 | Flyway | Spring 托管 | 🟢 托管 | Spring Boot 父 POM 自动管理 |
| 版本控制 | Git | — | — | `feature/M{XX}-{描述}` 分支 |
| 代码格式化（Java） | Spring Java Format | **0.0.43** | 🔴 精确锁 | pre-commit 自动格式化 |
| 代码格式化（前端） | Prettier | **3.4.2** | 🔴 精确锁 | pre-commit 自动格式化 |
| 静态检查 | CheckStyle | **10.17.0** | 🔴 精确锁 | pre-commit 0 error |
| 静态检查 | ESLint | **9.15.0** | 🔴 精确锁 | pre-commit 0 error |
| 依赖安全 | OWASP Dependency Check | **9.2.0** | 🔴 精确锁 | PR 合并前高危阻断 |
| 测试 | JUnit 5 + Mockito | Spring 托管 | 🟢 托管 | 覆盖率 ≥ 80% |
| 部署 | Docker Compose | **2.24** | 🟡 次版本锁 | `docker-compose up -d` |
| 监控 | Spring Boot Actuator | Spring 托管 | 🟢 托管 | `/health` 端点 |

**前端依赖版本**（写入 `package.json`）：

| 组件 | 版本 | 锁定级别 | 说明 |
|------|------|:--:|------|
| Node.js | **20 LTS** | 🔴 LTS 锁 | 运行时 |
| React | **18.3.1** | 🔴 次版本锁 | KB.md 基准版本。**禁止升级到 React 19**（19 移除 forwardRef/旧 Context，Antd 5 部分组件可能不兼容；KB.md 所有代码示例基于 18） |
| Ant Design | **5.22.3** | 🔴 次版本锁 | ProTable 基准版本 |
| Vite | **5.4.11** | 🔴 次版本锁 | 不追 v6/v7/v8 |
| TypeScript | **5.6.3** | 🔴 次版本锁 | 配套 React 18 |
| @tanstack/react-query | **5.62.0** | 🟡 次版本锁 | 服务端状态管理 |
| Zustand | **5.0.1** | 🟡 次版本锁 | 客户端状态管理 |
| axios | **1.7.9** | 🟡 次版本锁 | HTTP 客户端 |

**锁定级别说明**：
- 🔴 **精确锁/次版本锁**：版本号写入 `build.gradle` 或 `package.json`，AI 对话加载后以此为唯一真源
- 🟡 **次版本锁**：锁定 `主.次`，补丁版本可自动升级
- 🟢 **托管**：由父 POM 管理，不手动声明

### 4.3 Git 分支规范

```
main
 ├── feature/M01-project-skeleton   → PR 评审 → 合并 main
 ├── feature/M02-org-structure      → PR 评审 → 合并 main
 └── ...
```

- 分支名：`feature/M{XX}-{短描述}`
- 提交信息：`<type>: <描述>`（如 `feat: 实现号码分配悲观锁`）
- PR 要求：≥1 人 Review + CheckStyle 通过 + 测试通过
- 合并方式：Squash Merge → 删除特性分支

### 4.4 Pre-commit 检查

每次 `git commit` 前自动执行：

1. ✅ 代码格式化
2. ✅ CheckStyle/ESLint 0 error
3. ✅ 单元测试（当前模块）
4. ✅ 禁止 `System.out` / `console.log`
5. ✅ 禁止无日期/负责人的 TODO/FIXME

### 4.5 基础工程文件

M01 必须产出以下两个文件，后续所有模块不修改：

**`.editorconfig`**：
```ini
root = true
[*]
indent_style = space
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true
[*.java]
indent_size = 4
[*.{ts,tsx,js,jsx,json,css,scss}]
indent_size = 2
```

**`.gitignore`**：
```
# Java
build/
.gradle/
*.class
*.jar
*.war
!gradle/wrapper/gradle-wrapper.jar

# Node
node_modules/
dist/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Env
.env
.env.local
application-prod.yml
```

---

## 5. 27 模块开发计划（增强版）

> 保留 v2 依赖结构和模块划分，每个 Module 增加**安全自查清单**和**质量门禁**。
> **所有 Module 严格遵循 §1.4 的 6 步开发流程：合同→数据库→后端→契约验证→前端→门禁。**

### 5.1 依赖顺序

```
M01 骨架（含 AuditLogger + NotificationPublisher 空接口）
 └─ M02 组织架构
     ├─ M03 员工 → M04 认证（此后需登录，含 @PreAuthorize 模板）
     ├─ M05 成本中心
     └─ M06 号码基础（依赖 M02+M03+M04）
         ├─ M07 号池 / M08 区号
         ├─ M09 分配回收 ⚠️ / M10 状态变更 / M11 号码变更 / M12 导入
         │   └─ 均调用 M01 预定义空接口（通知+审计），M15 接入真实实现
         └─ M13 话机基础 → M14 话机操作

M15 通知 / M16 权限审查补漏 / M17 仪表盘
═══════════ Phase 2 ═══════════
M18 工单基础 → M19 流转拆单 → M20 操作迁移（功能开关控制 API 切换）
M21 月度快照 → M22 基础报表
═══════════ Phase 3 ═══════════
M23 账单分摊 → M24 异常检测
M25 发票OCR → M26 分发确认
M27 完整报表
```

---

### Phase 1：基础平台

#### M01 — 项目骨架

| 维度 | 内容 |
|------|------|
| **依赖** | 无 |
| **新建表** | 无（仅 Flyway 基线） |
| **分支** | `feature/M01-project-skeleton` |

**6 步开发流程**：

| 步 | 内容 | 产出 |
|----|------|------|
| **Step 1 合同** | 定义通用 DTO：`ApiResponse<T>`、`ResultCode` 枚举、`BaseEntity` 基类 + **错误码号段分配** + 前端 TS 类型对应 + **版本基线文件**：`build.gradle`（后端全部依赖版本）+ `package.json`（前端全部依赖版本，版本号见 §4.2） | `ApiResponse.java` + `types/common.ts` + `ErrorCode.java`（含号段划分）+ `build.gradle` + `package.json` |
| **Step 2 数据库** | Flyway 基线配置（无迁移文件）。约定：所有种子数据统一放在 `src/main/resources/db/seed/` 目录，按模块命名 `V{XX}__seed_{module}.sql`；**版本号按 Phase 分配并预留余量**：Phase 1 用 V01~V09（对应 M01-M09，**预留 V10-V17 共 8 个空号段**供 DDL 修补、hotfix 和多团队并行开发），Phase 2 用 V18~V24（M18-M24，**预留 V25-V30 共 6 个空号段**），Phase 3 用 V31~V33（M25-M27，**预留 V34-V50 共 17 个空号段**，Phase 3 早期交付快、后期功能复杂预留更多空间）。多团队并行开发时，各自在分配的号段内顺序递增，禁止跨 Phase 借用号段。Hotfix 迁移文件名格式：`V{XX}__{module}_hotfix_{描述}.sql`，XX 取下一个可用号段 | `application.yml` 中 Flyway 配置 |
| **Step 3 后端** | Gradle 项目 + `BusinessException` + `GlobalExceptionHandler` + `CorsConfig` + CheckStyle + OWASP 配置 + **预定义空接口**：`AuditLogger`（`log(action, operator, target, detail)` 空实现）+ `NotificationPublisher`（`publish(type, targetUser, title, content)` 空实现），供后续模块 M09-M14 调用，M15 替换为真实实现 | `gradle build` 成功 |
| **Step 4 契约验证** | `gradle bootRun` → `/health` 端点可用 | Actuator health 200 |
| **Step 5 前端** | Vite + React 18 + Antd 5 + axios + 路由骨架 + Prettier + ESLint | `npm run build` 成功 |
| **Step 6 门禁** | G1-G5（M01 无数据操作，G3/G5 自动通过） | 见验收 |

| **安全** | ☐ 无数据操作 |
| **验收** | `gradle build` 通过、`npm run build` 通过、CheckStyle 0 error、OWASP 无高危 |

**错误码号段分配**（写入 `ErrorCode.java`，各模块在所属号段内添加）：

| 号段 | 模块 | 示例 |
|:--|------|------|
| AUTH-001~099 | M04 认证授权 | AUTH-001 用户名不存在、AUTH-002 密码错误、AUTH-005 账号已锁定 |
| ORG-001~099 | M02 组织架构 | ORG-001 组织不存在、ORG-002 组织名重复、ORG-003 环检测 |
| EMP-001~099 | M03 员工管理 | EMP-001 工号已存在、EMP-002 工号格式错误 |
| PHONE-001~099 | M06 号码基础 | PHONE-001 号码不存在、PHONE-002 号码已分配 |
| PHONE-100~199 | M09 分配/回收 | PHONE-100 号码不可分配、PHONE-101 分机号重复 |
| PHONE-200~299 | M10 状态变更 | PHONE-200 状态不可变更、PHONE-201 禁止操作已拆机号码 |
| PHONE-300~399 | M11 号码变更 | PHONE-300 变更目标号码不可用、PHONE-301 双号码锁冲突 |
| POOL-001~099 | M07 分机号池 | POOL-001 号池区间重叠、POOL-002 号池耗尽 |
| DEVICE-001~099 | M13/M14 话机 | DEVICE-001 MAC 格式错误、DEVICE-002 话机已分配 |
| IMPORT-001~099 | M12 号码导入 | IMPORT-001 文件格式错误、IMPORT-002 导入超时 |
| WO-001~099 | M18/M19/M20 工单 | WO-001 工单不存在、WO-002 状态流转非法 |
| BILL-001~099 | M23/M24 账单 | BILL-001 账单月份不存在、BILL-002 分摊金额异常 |
| INV-001~099 | M25/M26 发票 | INV-001 发票识别失败、INV-002 发票已分发 |
| SYS-001~099 | M01 公共 | SYS-001 系统内部错误、SYS-002 参数校验失败 |

---

#### M02 — 组织架构

| 维度 | 内容 |
|------|------|
| **依赖** | M01 |
| **新建表** | `org_structure`（parent_id + type ENUM + level + path + status） |
| **分支** | `feature/M02-org-structure` |
| **交付** | Org entity + repository + OrgService（CRUD + TreeBuilder + path/level 自动计算 + depth 预校验 ≤25 层 + 同名/环/inactive 检测）+ OrgController（5 端点） |
| | OrgTree 组件 + 组织表单 |
| **种子** | 集团总部 org + 待分配部门 org |
| **安全** | ☐ JPA 参数化查询 ☐ 输入校验：名称 ≤100 字符 ☐ create/update/delete INFO 日志（含操作人+orgId） |
| **门禁** | 树形展示正确、同名/环检测生效、响应 < 200ms |

---

#### M03 — 员工管理

| 维度 | 内容 |
|------|------|
| **依赖** | M02 |
| **新建表** | `employee`（employee_no UNIQUE + org_id FK + status + phone/email） |
| **分支** | `feature/M03-employee-management` |
| **交付** | Employee entity + repository + CRUD + 工号校验（6 位字符）+ 手机/邮箱格式校验 + 虚拟员工（VIR-{部门}-{序号}）+ EmployeeController |
| | EmployeePage（ProTable）+ EmployeeForm |
| **种子** | VIR-总部-01(admin), VIR-总部-02(ops), VIR-总部-03(boss) |
| **安全** | ☐ 手机 `^1[3-9]\\d{9}$` ☐ 邮箱 `^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$` ☐ 工号 UK 约束 ☐ 日志不打印手机号 |
| **门禁** | 工号唯一、虚拟员工正确、格式校验生效 |

---

#### M04 — 认证授权

| 维度 | 内容 |
|------|------|
| **依赖** | M03 |
| **新建表** | `sys_user`（username UNIQUE + password_hash + role ENUM + scope_org_id + status + password_changed_at + login_fail_count + locked_until） |
| **分支** | `feature/M04-auth` |
| **交付** | JWT（HMAC-SHA256 + jti）+ SecurityConfig + JwtFilter + UserDetailsServiceImpl + AuthController（login/logout/me/change-password）+ 锁定策略（5 次→30 分钟→Admin 解锁）+ 首次强制改密 |
| | **权限基础设施**（随认证同时交付）：`@PreAuthorize` 注解模板 + 基础 `PermissionEvaluator`（scope 裁剪）+ 角色常量 `Role.ADMIN/OPS/FINANCE/BOSS` |
| | 此后所有 Module 的 Controller 创建时就必须加权限注解，M16 仅做全面审查和补漏 |
| | 登录页 + authStore + axios 拦截器 + 路由守卫 |
| **种子** | admin/admin123!、ops/ops123!、boss/boss123!、finance/finance123!（**仅 dev 环境**；首次登录 `forceChangePassword=true`，前端提示改密；生产环境由管理员初始化） |
| **安全** | ☐ bcrypt cost=12 ☐ JWT 密钥环境变量 ☐ 审计：登录成功/失败/锁定记 IP+用户+时间 ☐ 无密码明文日志 |
| **门禁** | 登录/登出/过期跳转正常、锁定/解锁/强制改密完整 |

---

#### M05 — 成本中心

| 维度 | 内容 |
|------|------|
| **依赖** | M02 + M04 |
| **新建表** | `cost_center_mapping`（org_id FK + cost_center_code UNIQUE + status） |
| **分支** | `feature/M05-cost-center` |
| **交付** | Entity + CRUD + 权限（仅 Finance 可写） |
| | CostCenterPage（ProTable + 表单） |
| **安全** | ☐ `@PreAuthorize("hasRole('FINANCE')")` ☐ org_id 关联校验 |
| **门禁** | 仅 Finance 可增删改、多对多关联正常 |

---

#### M06 — 号码基础

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 + M03（phone_number 关联 employee） |
| **新建表** | `phone_number`（version + extension_type + is_reentry + allocation_org_id）+ `phone_history`（永久）+ `phone_surrender_record`（永久） |
| **分支** | `feature/M06-phone-base` |
| **交付** | Phone entity + PhoneHistory entity + SurrenderRecord entity + PhoneRepository（列表/搜索/状态过滤）+ PhoneService（list/get/history）+ PhoneController（GET only 5 端点） |
| | PhoneTable（ProTable + 6 状态过滤）+ PhoneDetail（属性卡片 + 历史时间线）+ PhoneStatusBadge（6 色标签） |
| **安全** | ☐ 列表按 scope 过滤 ☐ history 不可修改/删除 ☐ 日志不含完整号码（脱敏） |
| **门禁** | 分页/搜索/过滤正常、历史时间线完整、状态标签颜色正确 |

---

#### M07 — 分机号池

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 |
| **新建表** | `extension_pool`（org_id FK + start/end_number） |
| **分支** | `feature/M07-extension-pool` |
| **交付** | Entity + CRUD + 重叠检测 + 用量计算 + 三色预警（绿≥30%/黄10-30%/红<10%）+ 阈值跨越通知 |
| | ExtensionPoolPage + 预警进度条 |
| **安全** | ☐ Ops 独占写操作 ☐ start ≤ end 校验 ☐ 号池区间不与已有重叠 |
| **门禁** | 重叠检测生效、预警颜色正确、跨越阈值触发通知 |

---

#### M08 — 区号对照

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02 |
| **新建表** | `area_code_org_mapping` |
| **分支** | `feature/M08-area-code` |
| **交付** | Entity + CRUD |
| | AreaCodePage |
| **安全** | ☐ Ops 独占写操作 ☐ 区号格式校验 |
| **门禁** | CRUD 正常 |

---

#### M09 — 号码分配/回收 ⚠️ 并发关键

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M07 + M08 + M03 |
| **新建表** | 无 |
| **分支** | `feature/M09-phone-allocate` |
| **交付** | PhoneRepository **专用锁**（`findByIdForUpdate`/`findByIdsForUpdate`）+ allocate（校验链：idle→分机号唯一→号池→悲观锁→分配→写 history→通知）+ reclaim（双轨回收：auto→idle / manual→归还号池+allocation_org_id→写 history） |
| **JPA 悲观锁实现方式** | `JpaRepository` 不支持 `findByIdForUpdate()`。正确实现：在 `PhoneRepository` 接口中自定义方法 `findByIdForUpdate(Long id)` + `@Query("SELECT p FROM PhoneNumber p WHERE p.id = :id")` + `@Lock(LockModeType.PESSIMISTIC_WRITE)`；或在 Service 层使用 `entityManager.find(PhoneNumber.class, id, LockModeType.PESSIMISTIC_WRITE)`。禁止直接调用不存在的方法名。 |
| | AllocateDialog + ReclaimDialog + 按钮可见性 |
| **P0 修复：**
> **ARCH-006 语法修复**：以下所有 `@Transactional(READ_COMMITTED)` 写法为 JPA 语法错误，正确写法为 `@Transactional(isolation = Isolation.READ_COMMITTED)`。
> **ARCH-007 超时配置**：JPA 悲观锁 `PESSIMISTIC_WRITE` 默认等待 50 秒不可接受，会导致数据库连接耗尽。必须在 `application.yml` 中配置 `spring.jps.properties.jakarta.persistence.lock.timeout=5000`（5 秒）或在 `@Lock` 注解中配合 `@QueryHints` 设置 ` javax.persistence.lock.timeout=5000`。并发场景下 5 秒内无法获取锁视为死锁/竞争异常，立即失败优于无限等待。
>
> **实现示例（正确）：**
> ```java
> // 方式1：Repository 层
> @Query("SELECT p FROM PhoneNumber p WHERE p.id = :id")
> @Lock(LockModeType.PESSIMISTIC_WRITE)
> @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
> Optional<PhoneNumber> findByIdForUpdate(Long id);
>
> // 方式2：application.yml 全局配置
> spring:
>   jpa:
>     properties:
>       jakarta.persistence.lock.timeout: 5000  # 5秒超时
> ```
>
> **安全** | ☐ `@Lock(PESSIMISTIC_WRITE)` + `@Transactional(isolation = Isolation.READ_COMMITTED)` ☐ `@QueryHints` 超时 5 秒 ☐ version 二道防线 ☐ 操作审计日志 |
| **隐式依赖** | ☐ 通知：调用 `NotificationPublisher.publish()`（M01 预定义空接口，M15 接入真实实现） ☐ 审计日志：调用 `AuditLogger.log()`（M01 预定义空接口） |
| **门禁** | **并发测试通过**（多线程同时分配同一号码不重复）、分机号双轨判定正确、history 记录完整 |

---

#### M10 — 号码状态变更

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M09 |
| **新建表** | 无 |
| **分支** | `feature/M10-phone-status` |
| **交付** | reserve/release（仅 idle）+ disable/enable（仅 idle）+ trouble（active↔stopped）+ surrender（二次确认→cancelled+写 surrender_record+re-entry 检测） |
| | ReserveDialog + DisableDialog + TroubleDialog + SurrenderDialog（二次确认弹窗）+ 按钮可见性 |
| **安全** | ☐ 状态迁移矩阵校验 ☐ surrender 二次确认弹窗 ☐ 操作审计日志 |
| **隐式依赖** | ☐ 通知/审计日志：同 M09 |
| **门禁** | 状态迁移全部合法路径通过、违规操作被拒绝、二次入库检测生效 |

---

#### M11 — 号码变更 ⚠️ 并发关键

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M09 |
| **新建表** | 无 |
| **分支** | `feature/M11-phone-change` |
| **交付** | change-user（分机号按新员工重判定）+ change-number（单 @Transactional + 双号码 `findByIdsForUpdate` + ORDER BY id ASC 防死锁）+ change-org + EMP-04 离职自动 reclaim（事务联动） |
| **JPA 悲观锁实现方式** | `findByIdsForUpdate(List<Long> ids)` 同样需自定义 `@Query` + `@Lock(PESSIMISTIC_WRITE)`。`ORDER BY id ASC` 必须在 SQL 层保证，防止 JVM 排序不原子导致死锁。 |
| | ChangeUserDialog + ChangeNumberDialog + ChangeOrgDialog + 离职确认弹窗 |
| **安全** | ☐ change-number 双锁 + ORDER BY ASC + `@QueryHints` 超时 5 秒 ☐ 离职回收事务原子性 ☐ 操作审计日志 |
| **隐式依赖** | ☐ 通知/审计日志：同 M09 |
| **门禁** | **并发 deadlock 测试通过**（多线程同号码对 + 超时 5 秒内感知死锁）、双号码操作原子性、离职回收联动正确 |

---

#### M12 — 号码导入

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M02 |
| **新建表** | `import_batch`（batch_id + total/success/fail_count + status + error_detail JSON） |
| **分支** | `feature/M12-phone-import` |
| **交付** | ImportService（@Async + 批量查询 HashMap+IN + 归一化 + 判重 + surrender 检测）+ batchInsert(100) + 进度轮询 API + **线程池配置**：`ThreadPoolTaskExecutor`（corePoolSize=2, maxPoolSize=4, queueCapacity=50, **rejectionPolicy=CallerRunsPolicy**），避免默认单线程导致导入串行；CallerRunsPolicy 保证任务不丢失（调用方线程执行），优于默认 AbortPolicy（直接抛 `RejectedExecutionException`，会导致正在导入的任务静默失败，触发 500 错误） |
| **冲突处理规则** | 导入号码与库中已有号码冲突时，按以下策略处理（Admin 在导入前选择）：`ERROR`（默认，报错拒绝导入）、`SKIP`（跳过冲突行，继续导入其余）、`OVERWRITE`（**仅允许覆盖 `idle` 状态的号码**；若库中号码为 `active/stopped/reserved` 状态则转为 `SKIP`，不静默覆盖；保留历史）。冲突判断：按 phone_number 唯一索引（号码值）。所有冲突行统一记入 `error_detail JSON`，不污染已有数据 |
| | ImportPage（上传→进度条→预览→确认→错误明细） |
| **安全** | ☐ 文件类型校验（仅 .xlsx/.xls） ☐ 文件大小限制（≤10MB） ☐ 异步超时 5 分钟 ☐ 导入失败不污染已有数据 |
| **门禁** | 100 条不超时、列优先匹配正确、失败行标记准确 |

---

#### M13 — 话机基础

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M04 |
| **新建表** | `phone_device`（mac_address + model + status ENUM + org_id FK）+ `device_phone_mapping`（M:N）+ `phone_device_history` |
| **分支** | `feature/M13-device-base` |
| **交付** | Device entity + repository + DeviceService（CRUD + MAC 归一化：去冒号+转大写+12 位十六进制 + 状态变更 + 写 history）+ DeviceController（16 端点） |
| | DeviceTable + DeviceDetail + DeviceForm |
| **安全** | ☐ MAC 格式校验（`^[0-9A-F]{12}$`）☐ 操作 history 不可删除 |
| **门禁** | MAC 归一化正确、列表筛选正常、history 记录完整 |

---

#### M14 — 话机操作

| 维度 | 内容 |
|------|------|
| **依赖** | M13 + M06 |
| **新建表** | 无 |
| **分支** | `feature/M14-device-operations` |
| **交付** | 分配/回收（scope 控制）+ 停用/启用 + 送修（自动解绑）+ 修复（→active）+ 报废（终态：**报废时若话机已绑定号码，自动执行该号码的 reclaim 操作**→idle 状态+写 history，确保号码资源不泄漏）+ 绑定/解绑号码（按分机号，跨员跨组织允许，无分机号禁绑）+ 离职自动回收 + 操作通知 |
| | 6 类操作 Dialog + 按钮可见性（5 状态矩阵） |
| **安全** | ☐ 送修自动解绑 ☐ 报废不可逆 ☐ 操作审计日志 |
| **隐式依赖** | ☐ 通知/审计日志：同 M09 |
| **门禁** | 送修自动解绑、跨组织绑定允许、离职回收联动 |

---

#### M15 — 通知系统

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M09-M14 |
| **新建表** | `sys_notification`（type + title + content + target_user + related_id + is_read） |
| **分支** | `feature/M15-notification` |
| **交付** | NotificationService（统一入口）+ 规则：直属 Admin（无则上查）+ 所有 Ops + 号码/话机/号池操作触发 |
| **通知触发矩阵** | 通知触发必须覆盖以下所有场景（实现时不得遗漏）：<br>• **号码分配** → 被分配员工（本人）+ 直属 Admin（查看范围包含该员工）<br>• **号码回收** → 被回收员工 + 直属 Admin<br>• **拆机二次确认** → 申请人 + 直属 Admin<br>• **号码 reserve/release/disable/enable/trouble** → 号码当前持有人（如已分配）+ 直属 Admin<br>• **号码 change-user/change-number/change-org** → 变更前后员工 + 双方直属 Admin<br>• **号码导入完成** → 导入操作人 + 失败行所属组织 Admin<br>• **话机送修** → 话机当前使用人（如已分配）+ 直属 Admin<br>• **话机报废** → 直属 Admin（话机不可逆报废通知）<br>• **话机绑定/解绑号码** → 号码当前持有人 + 话机使用人（如不同）<br>• **号池耗尽预警（利用率达 100%）** → 该号池所属组织 Ops + Admin<br>• **工单创建** → 工单处理人<br>• **工单状态变更** → 工单请求人 + 处理人<br>• **号池预警（红/黄）** → 该号池所属组织 Ops + Admin<br>• **异常账单标记** → 财务角色<br>• **离职回收通知** → 直属 Admin（收到员工离职后的号码回收完成通知）<br>• **拆机后 re-entry 号码可再分配** → 直属 Admin（号码从 cancelled→idle 可再分配通知）<br>所有通知内容中的电话号码必须脱敏（`138****1234` 格式） |
| | NotificationList + 顶部 Badge（未读数） |
| **安全** | ☐ 通知内容不含敏感数据（号码脱敏） ☐ 仅推送给有权用户 |
| **门禁** | 操作触发通知正确送达、未读标记准确 |

---

#### M16 — 权限收尾（审查补漏，非从零搭建）

| 维度 | 内容 |
|------|------|
| **依赖** | M04 + M02-M15 |
| **新建表** | 无 |
| **分支** | `feature/M16-permissions` |
| **交付** | 审查所有 Controller 的权限注解完整性 + 精细化 scope 裁剪（Admin 只能操作本部门及下级）+ Boss/Finance 全系统只读**最终确认** + 权限审计（哪些端点无注解） |
| | 按钮可见性（角色+scope）+ 组织树（完整树+scope 外置灰+可点击只读） |
| **安全** | ☐ 每个 API 端点有对应权限注解（审查） ☐ Admin scope 外数据不可操作 ☐ Boss/Finance 写操作全拦截 |
| **门禁** | Admin 无法操作 scope 外数据、Boss/Finance 写操作被拒、组织树置灰只读正确 |

> M04 已交付权限基础设施。本模块只做**全面审查 + scope 细化**，不为每个 Controller 从零加注解。

---

#### M17 — 仪表盘 & 功能开关

| 维度 | 内容 |
|------|------|
| **依赖** | M02-M16 |
| **新建表** | `sys_feature_flag`（feature_key + enabled + description） |
| **分支** | `feature/M17-dashboard` |
| **交付** | DashboardService（号码统计/组织统计/操作概览 API）+ FeatureFlagService + `@ConditionalOnFeatureFlag` |
| | Dashboard 页面 + 功能开关管理页 |
| **安全** | ☐ Dashboard 数据按 scope 过滤 ☐ 功能开关仅 Admin 可操作 |
| **门禁** | 仪表盘数据与实际一致、功能开关实时生效 |

---

### Phase 2：工单系统

#### M18 — 工单基础

| 维度 | 内容 |
|------|------|
| **依赖** | Phase 1 完成 |
| **新建表** | `work_order`（work_order_no UNIQUE + type ENUM + status ENUM + priority + requester + handler + batch_id）+ `work_order_item`（phone/device/employee 快照 + action + from/to 字段） |
| **分支** | `feature/M18-work-order` |
| **交付** | WorkOrder entity + WorkOrderItem entity + CRUD + 编号生成 + WorkOrderController |
| | WorkOrderTable + WorkOrderDetail + WorkOrderForm |
| **安全** | ☐ 工单编号全局唯一 ☐ 快照字段不可篡改 ☐ Admin 只能查看 scope 内工单 |
| **门禁** | CRUD 正常、编号唯一、快照记录正确 |

---

#### M19 — 工单流转 & 批量拆单

| 维度 | 内容 |
|------|------|
| **依赖** | M18 |
| **新建表** | 无 |
| **分支** | `feature/M19-work-order-flow` |
| **交付** | 流转：pending→accepted→processing→completed→archived + 批量拆单（跨部门按 org_id 分组→子工单+共享 batch_id）+ 消息推送（进度→相关 Admin+Ops） |
| | 批量创建页（多选号码→预览拆分→提交）+ 处理页 + 归档页 |
| **安全** | ☐ 状态流转校验（禁止跳跃）☐ 批量工单拆单正确性 ☐ 操作审计日志 |
| **隐式依赖** | ☐ 通知/审计日志：同 M09 |
| **门禁** | 流转无跳跃、跨部门正确拆分、消息推送达标 |

---

#### M20 — 号码操作迁移工单驱动

| 维度 | 内容 |
|------|------|
| **依赖** | M18 + M09 + M10 + M11 |
| **新建表** | 无 |
| **分支** | `feature/M20-work-order-migration` |
| **交付** | 7 类工单（分配/过户/换号/转移/回收/拆机/停复机）+ 状态变更仅通过工单触发 + phone_history.work_order_no 关联 |
| | 调整操作页面：操作→创建工单（预填）→提交→工单列表追踪 |
| **安全** | ☐ 禁止绕过工单直接操作 ☐ 操作结果与工单关联可追溯 |
| **API 迁移策略** | 通过 `sys_feature_flag.work_order_driven` 开关控制：`false`（默认）→ Phase 1 直接操作 API 仍可用；Admin 手动开启 `true` → 工单驱动激活。**补充策略**：<br>• **灰度发布**：`sys_feature_flag` 表增加 `scope_org_id` 字段（**此字段归属 M20，在 M20 自身迁移文件 `V20__work_order_migration.sql` 中创建字段并加注释描述，而非借用 M17 的迁移文件**），支持按组织灰度（先试点组织 → 全量），而非全开全关<br>• **绕过防护**：开关 `true` 时，后端直接操作 API（M09-M11 原生端点）物理加 `@PreAuthorize("hasAnyRole('ADMIN','OPS') AND @scopeService.check(#scopeOrgId)")` 注解，**禁止使用未定义的 `SUPER_ADMIN` 角色**（该角色未在 M04 角色体系中定义）；Admin 权限通过 `scopeOrgId` 参数校验，而非超级角色<br>• **回滚机制**：开关状态切换前，系统自动触发一次 `phone_snapshot` 快照备份；切换历史记录（含操作人/时间/开关值）存入 `sys_feature_flag_log` 表；`true→false` 回滚后，旧 API 立即恢复，无需代码部署<br>• **旧端点废弃**：Phase 3 稳定后，原生操作端点加 `@Deprecated` 注解 + 响应头 `X-API-Deprecated: true`，6 个月后物理删除 |
| **门禁** | 所有号码操作只能通过工单、操作可追溯到工单 |

---

#### M21 — 月度快照

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M02 |
| **新建表** | `phone_snapshot`（snapshot_month + phone_id UK + status + org_id/org_name 快照 + cost_center_code 快照 + is_surrendered + is_allocatable） |
| **分支** | `feature/M21-phone-snapshot` |
| **交付** | Entity + SnapshotService（月末自动→查 active/stopped/cancelled→写快照+匹配成本中心）+ `@Scheduled` 定时 + 失败重试 3 次 + 手动触发 API |
| **安全** | ☐ 快照不可修改 ☐ 定时任务幂等（重复执行不重复写入） |
| **门禁** | 月末自动生成、成本中心匹配正确、失败重试生效 |

---

#### M22 — 基础报表

| 维度 | 内容 |
|------|------|
| **依赖** | M06 + M18 + M21 |
| **新建表** | 无 |
| **分支** | `feature/M22-basic-reports` |
| **交付** | 号码资产报表（按组织/状态聚合+导出 Excel）+ 工单统计（按类型/时间/处理人+平均时长） |
| | ReportPage（筛选+表格+图表+导出） |
| **安全** | ☐ 报表数据按 scope 过滤 ☐ 导出 Excel 不包含 scope 外数据 |
| **门禁** | 统计数据与实际一致、Excel 导出格式正确 |

---

### Phase 3：费用 & 发票

#### M23 — 账单导入 & 分摊

| 维度 | 内容 |
|------|------|
| **依赖** | Phase 2 完成 + M05 |
| **新建表** | `bill_raw`（bill_month + phone_number + charge_amount + raw_data JSON + import_status）+ `bill_allocation`（关联 bill_raw + snapshot_org + cost_center + anomaly_flag + 确认字段） |
| **分支** | `feature/M23-bill-allocation` |
| **交付** | BillService（Excel 解析→列名映射→存 raw_data）+ AllocationService（匹配 phone_snapshot→归属部门+成本中心）+ 多角色确认（行政确认归属/金额→财务确认异常→财务提交） |
| | 导入页 + 分摊确认页（行政）+ 财务确认页 |
| **安全** | ☐ Excel 文件类型/大小校验 ☐ 账单金额精度 DECIMAL(12,2) ☐ 确认流程不可跳过 ☐ 操作审计日志 |
| **门禁** | Excel 解析兼容常见模板、分摊匹配正确、多角色确认流转正常 |

---

#### M24 — 异常检测

| 维度 | 内容 |
|------|------|
| **依赖** | M23 |
| **新建表** | 无 |
| **分支** | `feature/M24-anomaly-detection` |
| **交付** | 同比/环比金额对比 + 差异超阈值（可配置，默认 30%）→自动标记 anomaly_flag + 异常查询 API |
| | 异常账单列表（金额红色高亮） |
| **安全** | ☐ 阈值仅 Admin 可配置 ☐ 异常标记不可手动取消（需财务确认） |
| **门禁** | 阈值可配置、异常标记准确 |

---

#### M25 — 发票上传 & OCR

| 维度 | 内容 |
|------|------|
| **依赖** | Phase 2 完成 + M02 |
| **新建表** | `invoice`（invoice_no UNIQUE + source_org/recipient_org + amount + status + ocr_text + ocr_confidence）+ `invoice_file`（file_name + file_path + file_size + md5） |
| **分支** | `feature/M25-invoice-ocr` |
| **交付** | InvoiceService（上传→存文件→OCR 识别公司名称→匹配 org_structure.name→自动/待人工）+ OCR 集成 |
| | 上传页（批量 PDF + 命名校验）+ OCR 结果展示 + 手动匹配 |
| **安全** | ☐ 文件 MIME 校验（仅 PDF） ☐ 文件大小 ≤ 50MB ☐ OCR 文本不暴露在日志 ☐ md5 防重 |
| **门禁** | PDF 上传正常、OCR 识别率 ≥ 80%（人工兜底）、匹配失败标记待确认 |

---

#### M26 — 发票分发 & 确认

| 维度 | 内容 |
|------|------|
| **依赖** | M25 |
| **新建表** | `invoice_distribution`（invoice_id + recipient_user + status）+ `subsidiary_reconciliation`（bill_month + subsidiary_org + total_amount + reconciliation_status） |
| **分支** | `feature/M26-invoice-distribution` |
| **交付** | DistributionService（自动分发→通知子公司财务 + 匹配失败→通知集团财务人工）+ ReconciliationService（线下对账：导出→签字上传→确认） |
| | 分发管理页 + 发票确认页 + 对账页 |
| **安全** | ☐ 分发目标校验 ☐ 确认操作记录操作人+时间 ☐ 审计日志 |
| **门禁** | 自动分发率 ≥ 90%、确认记录完整、对账状态流转正确 |

---

#### M27 — 完整报表

| 维度 | 内容 |
|------|------|
| **依赖** | M22 + M23 + M24 + M26 |
| **新建表** | 无 |
| **分支** | `feature/M27-full-reports` |
| **交付** | 月度分摊报表 + 发票收发统计 + 异常账单报表 + 统一导出 Excel |
| | ReportCenter（多 Tab：号码资产/分摊/发票/异常/工单）+ 通用筛选 + 图表 + 导出 |
| **安全** | ☐ 报表按角色 scope 过滤 ☐ 导出不含 scope 外数据 ☐ 敏感金额脱敏（可选） |
| **门禁** | 5 类报表数据一致、筛选联动正确、Excel 导出完整 |

---

## 6. 质量门禁

### 6.1 模块级门禁（每个 Module）

```
开发完成 → 自测 → 下列 5 项全部通过 → 可提交 PR
```

| # | 门禁 | 标准 | 阻断级别 |
|----|------|------|:--:|
| G1 | 代码格式 | CheckStyle 0 error / ESLint 0 error | 🔴 阻断 |
| G2 | 单元测试 | **分层覆盖率**：Service 层 ≥ 90%（核心业务逻辑）、Controller 层 ≥ 70%（路由适配），Repository 层不强制（由集成测试覆盖）；全部通过；禁止只有 happy path（需覆盖边界/异常场景）；**CI 自动化**：JaCoCo 覆盖率报告集成到 GitHub Actions，PR 合并前覆盖率必须达标，报告存档于 `build/reports/jacoco/`；GitHub Actions workflow 示例：<br>```yaml<br>  - name: Run tests with JaCoCo<br>    run: |
      ./gradlew test jacocoTestReport<br>  - name: Upload coverage report<br>    uses: actions/upload-artifact@v4<br>    with:<br>      name: jacoco-report<br>      path: build/reports/jacoco/test/html/index.html<br>```<br>禁止：`./gradlew test` 不生成覆盖率 → PR 仍可合并（门禁失效） | 🔴 阻断 |
| G3 | 安全自查 | 模块安全清单全部 ☐ → ☑ | 🔴 阻断 |
| G4 | 依赖扫描 | OWASP 无高危漏洞（CVSS<7.0） | 🔴 阻断 |
| G5 | 日志检查 | 无敏感数据打印、变更操作有 INFO 日志 | 🟡 警告 |

### 6.2 集成门禁（每 3 个 Module）

```
M01-M03 → 集成回归 → M04-M06 → 集成回归 → ...
         ↑ 新对话：加载 KB+v3，跑回归 ← C4 约束
```

| # | 门禁 | 标准 | 约束 |
|----|------|------|------|
| I1 | 接口联调 | 前后端全部端点正常响应 | C2/C3 |
| I2 | 数据一致性 | 关联表外键约束无孤儿数据 | — |
| I3 | 权限回归 | 角色+scope 权限矩阵全部通过 | — |
| I4 | 新会话回归 | **新对话**加载 KB+v3 → curl 全集端点 → 全部 200 + Flyway 版本连续 + 无孤儿 FK | C4。通过标准：新对话中所有端点响应符合 DTO，不出现 500/404 |
| I5 | 数据库迁移 | Flyway 版本号连续，回滚可恢复 | — |

### 6.3 Phase 级门禁（Phase 收尾）

| Phase | 门禁 | 标准 |
|-------|------|------|
| Phase 1 | 全量功能测试 | 85 功能点全部通过（详见 `PhoneBiz-Phase1-需求清单.md` 功能点汇总；若文件不存在，以 `PhoneBiz-KNOWLEDGE_BASE.md` 功能清单为准） |
| | 性能基准 | 号码列表 < 2s、组织树 < 1s、导入 100 条 < 5min |
| | 并发压测 | allocate 100 并发无重复分配 **+ M11 change-number 双锁 deadlock 测试（10 并发同一号码对）+ 号池耗尽并发抢号测试**；工具建议 JMeter 或 Gatling，压测脚本存档于 `docs/performance-tests/`（**目录已创建，含 JMeter 脚本模板 `phonebiz-allocate-concurrent.jmx`**，Phase 门禁前需补充完整脚本） |
| | 安全测试 | SQL 注入/XSS/CSRF 测试全部通过（参考 `docs/security-test-cases.md` 测试用例清单：SQL 注入覆盖所有入参（`' OR 1=1 --`、单引号转义等）；XSS 覆盖所有文本输入（`<script>alert(1)</script>`）；CSRF 覆盖所有状态变更端点（POST/PUT/DELETE），使用 Burp Suite 或 ZAP 扫描；**文档已创建，含完整测试用例清单、安全防护实现要求和执行记录表**） |
| Phase 2 | 工单全流程 | 7 类工单 + 批量拆单 + 状态流转全路径覆盖 |
| | 快照准确性 | 快照数据与实时数据偏差 < 1% |
| Phase 3 | 账单分摊准确率 | 自动匹配率 ≥ 95% |
| | 发票 OCR 准确率 | 公司名称识别率 ≥ 80%（人工兜底） |
| | 报表对账 | 报表汇总金额与账单原始数据一致 |

### 6.4 发布门禁（生产上线）

| # | 门禁 | 标准 |
|----|------|------|
| R1 | 全量回归 | Phase 收尾测试全部通过 |
| R2 | 数据备份 | 生产数据库备份完成且可恢复 |
| R3 | 回滚预案 | Docker Compose 一键回滚脚本就绪 |
| R4 | 监控就绪 | `/health` 端点 + 关键 ERROR 日志告警 |
| R5 | 审批确认 | 负责人审批通过 |

---

## 附录

### A. 文档对照

| 本文档定位 | 旧文档关系 |
|-----------|-----------|
| **权威开发规范** | 取代 `PhoneBiz-开发阶段重排_v2.md` 为执行依据 |
| 模块细节 | 保留参考 `PhoneBiz-TASKS.md` + `PhoneBiz-Phase1-需求清单.md` |
| 架构设计 | 保留参考 `PhoneBiz-Phase1-架构设计.md` + `PhoneBiz-KNOWLEDGE_BASE.md` |
| 数据库 | 保留参考 `PhoneBiz数据库设计_DDL.sql` |

### B. 金融标准适配说明

| 金融标准 | PhoneBiz 处理 | 原因 |
|---------|:--:|------|
| 国密 SM2/SM3/SM4 | ❌ 不采用 | 非银行核心系统，无监管强制要求 |
| 4 环境物理隔离 | → 3 环境（dev/test/prod） | 内部系统，预发与测试合并 |
| 代码评审 ≥ 2 人 | → ≥ 1 人 | 团队规模限制，保留评审流程 |
| 渗透测试 | → 安全自查 + OWASP 扫描 | Phase 收尾补充渗透测试 |
| AI 模型开发规范 | ❌ 不适用 | PhoneBiz 无 AI 模型 |

### C. I4 回归测试脚本模板

新对话回归时使用以下脚本，按顺序 curl 当前已完成的所有端点：

```bash
#!/bin/bash
# PhoneBiz 集成回归脚本（新对话验证用）
# 使用方式：bash regression.sh M01..M{xx} ~/path/to/endpoints.txt

BASE="http://localhost:8080"
TOKEN=""  # 先 login 获取
# 数据库连接（孤儿 FK 检查用）
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_USER="${DB_USER:-phonebiz}"
DB_PASS="${DB_PASS:-phonebiz123}"
DB_NAME="${DB_NAME:-phonebiz}"

echo "=== Step 1: 登录 ==="
RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123!"}')
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null)
if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，请检查："
  echo "  1. 后端服务是否启动（gradle bootRun）"
  echo "  2. 账号密码是否正确（admin/admin123!）"
  echo "  Response: $RESP"
  exit 1
fi
echo "Token: ${TOKEN:0:20}..."

echo "=== Step 2: 回归所有端点 ==="
FAIL=0

# M02 组织架构
echo -n "M02 GET /api/orgs              "; curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/api/orgs" | grep -q "200" && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M02 GET /api/orgs/1            "; curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/api/orgs/1" | grep -q "200" && echo " ✅" || { echo " ❌"; FAIL=1; }

# M03 员工管理
echo -n "M03 GET /api/employees         "; curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/api/employees" | grep -q "200" && echo " ✅" || { echo " ❌"; FAIL=1; }

# M09 号码分配/回收
echo -n "M09 POST /api/phones/1/allocate      "; curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$BASE/api/phones/1/allocate" -d '{"employeeNo":"VIR-001"}' | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M09 POST /api/phones/1/reclaim      "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/phones/1/reclaim" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# M10 号码状态变更
echo -n "M10 POST /api/phones/1/reserve       "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/phones/1/reserve" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M10 POST /api/phones/1/release       "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/phones/1/release" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M10 POST /api/phones/1/disable       "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/phones/1/disable" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M10 POST /api/phones/1/trouble        "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/phones/1/trouble" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# M11 号码变更
echo -n "M11 POST /api/phones/change-user     "; curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$BASE/api/phones/change-user" -d '{"phoneId":1,"newEmployeeNo":"VIR-002"}' | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M11 POST /api/phones/change-number   "; curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$BASE/api/phones/change-number" -d '{"phoneId1":1,"phoneId2":2}' | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# M12 号码导入
echo -n "M12 POST /api/imports/upload        "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/imports/upload" -F "file=@/tmp/test.xlsx" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }
echo -n "M12 GET  /api/imports/batch/{id}     "; curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/imports/batch/1" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# M13 话机基础
echo -n "M13 GET  /api/devices               "; curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/devices" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# M14 话机操作
echo -n "M14 POST /api/devices/1/allocate     "; curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/api/devices/1/allocate" | grep -q '"code":200' && echo " ✅" || { echo " ❌"; FAIL=1; }

# ... 按 Module 顺序追加新端点 ...

echo "=== Step 3: Flyway 版本检查 ==="
echo "--- 迁移记录 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null || echo "（MySQL 未连接，请手动检查 Flyway 版本连续性）"
echo ""
echo "--- 版本号连续性 ---"
echo "检查：version 列是否连续（如 1→2→3），无跳号"

echo "=== Step 4: FK 孤儿检查 ==="
echo "--- phone_number.org_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM phone_number WHERE org_id IS NOT NULL AND org_id NOT IN (SELECT id FROM org_structure);" 2>/dev/null
echo "--- phone_number.employee_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM phone_number WHERE employee_id IS NOT NULL AND employee_id NOT IN (SELECT id FROM employee);" 2>/dev/null
echo "--- phone_history.phone_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM phone_history WHERE phone_id IS NOT NULL AND phone_id NOT IN (SELECT id FROM phone_number);" 2>/dev/null
echo "--- employee.org_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM employee WHERE org_id IS NOT NULL AND org_id NOT IN (SELECT id FROM org_structure);" 2>/dev/null
echo "--- cost_center_mapping.org_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM cost_center_mapping WHERE org_id IS NOT NULL AND org_id NOT IN (SELECT id FROM org_structure);" 2>/dev/null
echo "--- extension_pool.org_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM extension_pool WHERE org_id IS NOT NULL AND org_id NOT IN (SELECT id FROM org_structure);" 2>/dev/null
echo "--- phone_device.org_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM phone_device WHERE org_id IS NOT NULL AND org_id NOT IN (SELECT id FROM org_structure);" 2>/dev/null
echo "--- device_phone_mapping.device_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM device_phone_mapping WHERE device_id IS NOT NULL AND device_id NOT IN (SELECT id FROM phone_device);" 2>/dev/null
echo "--- device_phone_mapping.phone_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM device_phone_mapping WHERE phone_id IS NOT NULL AND phone_id NOT IN (SELECT id FROM phone_number);" 2>/dev/null
echo "--- work_order.requester_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM work_order WHERE requester_id IS NOT NULL AND requester_id NOT IN (SELECT id FROM employee);" 2>/dev/null
echo "--- work_order.handler_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM work_order WHERE handler_id IS NOT NULL AND handler_id NOT IN (SELECT id FROM employee);" 2>/dev/null
echo "--- work_order_item.work_order_id 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM work_order_item WHERE work_order_id IS NOT NULL AND work_order_id NOT IN (SELECT id FROM work_order);" 2>/dev/null
echo "--- sys_notification.target_user 孤儿 ---"
mysql -h127.0.0.1 -u$DB_USER -p$DB_PASS $DB_NAME -e "SELECT COUNT(*) AS orphan_count FROM sys_notification WHERE target_user IS NOT NULL AND target_user NOT IN (SELECT id FROM sys_user);" 2>/dev/null
echo "所有 orphan_count = 0 才通过，出现 >0 则说明存在孤儿 FK"

if [ $FAIL -eq 0 ]; then
  echo "✅ 回归通过"
else
  echo "❌ 回归失败，请检查上述 ❌ 标记的端点"
fi
```

> 每完成 3 个 Module 后，在 `regression.sh` 中追加新端点行。新对话只需 `bash regression.sh` 即可完成 I4 门禁。

> AI生成