# PhoneBiz 技术栈按V3版更新计划

> **执行依据：** [PhoneBiz-金融级开发规范_v3.md](file:///home/data/apps/phone_ip/20260510需求/PhoneBiz-金融级开发规范_v3.md)
> **执行原则：** 契约优先、后端先行、严格门禁
> **执行策略：** 完全重写，不基于现有项目修补

---

## 总体目标

将 PhoneBiz 项目技术栈从现有状态（Spring Boot 2.7 + MyBatis Plus + Maven + Java 8 + JS）完全升级到 v3 金融级规范要求（Spring Boot 3.2 + JPA + Gradle + Java 17 + TS），并完成 Phase 1 17 个模块的开发。

---

## 技术栈确认（严格按 §4.2）

### 后端技术栈

| 组件 | 版本 | 说明 | 锁定级别 |
|------|------|------|--------|
| **Java** | 17 LTS | OpenJDK | 🔴 精确 |
| **Spring Boot** | 3.2.5 |  | 🔴 精确 |
| **Gradle** | 8.7 |  | 🔴 精确 |
| **JPA/Hibernate** | Spring 托管 |  | 🟢 托管 |
| **Flyway** | Spring 托管 | 数据库迁移 | 🟢 托管 |
| **MySQL** | 8.0.x |  | 🟡 次版本 |
| **Spring Security** | Spring 托管 |  | 🟢 托管 |
| **JWT** | jjwt 0.12.3 |  | 🔴 精确 |
| **CheckStyle** | 10.17.0 | 代码格式 | 🔴 精确 |
| **Spring Java Format** | 0.0.43 |  | 🔴 精确 |
| **OWASP Dependency Check** | 9.2.0 | 安全扫描 | 🔴 精确 |
| **Docker Compose** | 2.24 | 部署 | 🟡 次版本 |

### 前端技术栈

| 组件 | 版本 | 说明 | 锁定级别 |
|------|------|------|--------|
| **Node.js** | 20 LTS |  | 🔴 精确 |
| **React** | 18.3.1 | ❌ **禁止升级到 19** | 🔴 次版本 |
| **Antd** | 5.22.3 |  | 🔴 次版本 |
| **Vite** | 5.4.11 |  | 🔴 次版本 |
| **TypeScript** | 5.6.3 |  | 🔴 次版本 |
| **Zustand** | 5.0.1 | 状态管理 | 🟡 次版本 |
| **@tanstack/react-query** | 5.62.0 | 服务端状态 | 🟡 次版本 |
| **axios** | 1.7.9 | HTTP 客户端 | 🟡 次版本 |
| **Prettier** | 3.4.2 | 格式化 | 🔴 精确 |
| **ESLint** | 9.15.0 | 静态检查 | 🔴 精确 |

---

## Phase 1 完整执行计划（17个模块）

### 总体依赖关系
```
M01 骨架
 └─ M02 组织架构
     ├─ M03 员工 → M04 认证
     ├─ M05 成本中心
     └─ M06 号码基础
         ├─ M07 号池 / M08 区号
         ├─ M09 分配/回收 ⚠️ / M10 状态变更 / M11 号码变更 / M12 导入
         └─ M13 话机 → M14 话机操作
M15 通知 / M16 权限收尾 / M17 仪表盘
```

---

### Module 01: 项目骨架（当前已部分完成）

**状态：** 部分完成，待补充

#### 6步执行计划

| 步骤 | 任务 | 交付物 |
|------|------|--------|
| **Step 1 合同** | 定义通用 DTO、Error Code 枚举、BaseEntity、前端 TS 类型 | `ApiResponse.java`, `ErrorCode.java`, `types/common.ts` |
| **Step 2 数据库** | Flyway 基线配置（无迁移），约定版本号规则 | `application.yml` Flyway 配置 |
| **Step 3 后端** | 完整 Gradle 项目、BusinessException、GlobalExceptionHandler、CorsConfig、AuditLogger/NotificationPublisher 空接口、CheckStyle/OWASP 配置 | `gradle build` 通过 |
| **Step 4 契约验证** | `/health` 端点可用 | Actuator 200 |
| **Step 5 前端** | Vite + React 18 + Antd 5 + 路由骨架 + Prettier + ESLint 配置 | `npm run build` 通过 |
| **Step 6 门禁** | G1-G5 检查通过（无数据操作，G3/G5 自动通过） | |

#### 详细任务清单

1. **完善 backend/build.gradle**
   - 添加所有依赖版本锁定
   - 配置 CheckStyle 和 OWASP Dependency Check
   - 配置 JaCoCo 覆盖率（Service ≥90%, Controller ≥70%）

2. **创建通用类**
   - `ApiResponse<T>`: 通用响应包装
   - `ErrorCode`: 完整错误码枚举（按 §5.1 号段分配）
   - `BusinessException`: 业务异常
   - `GlobalExceptionHandler`: 全局异常处理
   - `BaseEntity`: JPA 实体基类（id, createdBy, createdDate, updatedBy, updatedDate）
   - `AuditLogger`: 空接口
   - `NotificationPublisher`: 空接口

3. **配置文件**
   - `application.yml`: 完整配置（Flyway、JPA、Actuator、CORS）
   - `.editorconfig`: 已完成
   - `.gitignore`: 已完成
   - CheckStyle 配置文件 `config/checkstyle/checkstyle.xml`

4. **前端基础**
   - 完整 `package.json`（已完成）
   - Vite 配置文件
   - TS 类型定义 `types/common.ts`
   - Prettier 配置
   - ESLint 配置
   - 路由骨架 `App.tsx`, `main.tsx`
   - axios 配置和拦截器

5. **Flyway 约定**
   - Phase 1: V01-V09（M01-M09），预留 V10-V17 空号段
   - Phase 2: V18-V24，预留 V25-V30
   - Phase 3: V31-V33，预留 V34-V50
   - Seed 数据: `src/main/resources/db/seed/V{XX}__seed_{module}.sql`
   - Hotfix: `V{XX}__{module}_hotfix_{description}.sql`

---

### Module 02: 组织架构

#### 交付
- `OrgStructure` entity + repository
- OrgService: CRUD + TreeBuilder + path/level 自动计算 + 环/同名检测
- OrgController: 5 个端点
- 前端：OrgTree 组件 + 组织表单
- Flyway 迁移: `V02__org_structure.sql`

#### 表结构
```sql
CREATE TABLE org_structure (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT UNSIGNED NULL,
    name VARCHAR(100) NOT NULL,
    type ENUM('group', 'subsidiary', 'dept') NOT NULL,
    level INT NOT NULL,
    path VARCHAR(500) NOT NULL,
    status ENUM('active', 'inactive') NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES org_structure(id),
    INDEX idx_parent (parent_id),
    INDEX idx_path (path)
);
```

#### 门禁
- 树形展示正确
- 同名/环检测生效
- 响应 <200ms

---

### Module 03: 员工管理

#### 交付
- `Employee` entity + repository
- EmployeeService: CRUD + 工号校验（6位）+ 手机/邮箱格式校验 + 虚拟员工
- EmployeeController
- 前端：EmployeePage（ProTable）+ EmployeeForm
- Flyway 迁移: `V03__employee.sql`
- Seed: VIR-总部-01, VIR-总部-02, VIR-总部-03

#### 表结构
```sql
CREATE TABLE employee (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_no VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    org_id BIGINT UNSIGNED NOT NULL,
    position VARCHAR(50) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    status ENUM('active', 'inactive') NOT NULL,
    entry_date DATE NULL,
    leave_date DATE NULL,
    is_virtual TINYINT(1) NOT NULL DEFAULT 0,
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (org_id) REFERENCES org_structure(id),
    INDEX idx_org (org_id),
    UNIQUE KEY uk_employee_no (employee_no)
);
```

---

### Module 04: 认证授权（核心安全模块）

#### 交付
- `SysUser` entity + repository
- Spring Security 配置 + JwtFilter + UserDetailsServiceImpl
- JWT（HMAC-SHA256 + jti）
- AuthController: login/logout/me/change-password
- 锁定策略（5次失败→30分钟→Admin解锁）
- 首次强制改密
- 权限基础设施：`@PreAuthorize` 注解模板 + `PermissionEvaluator` + Role 常量
- 前端：登录页 + authStore + axios 拦截器 + 路由守卫
- Flyway 迁移: `V04__sys_user.sql`

#### 表结构
```sql
CREATE TABLE sys_user (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    employee_no VARCHAR(20) UNIQUE NOT NULL,
    role ENUM('admin', 'ops', 'finance', 'boss') NOT NULL,
    scope_org_id BIGINT UNSIGNED NULL,
    status ENUM('active', 'inactive') NOT NULL,
    login_fail_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    password_changed_at DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (employee_no) REFERENCES employee(employee_no),
    FOREIGN KEY (scope_org_id) REFERENCES org_structure(id),
    INDEX idx_username (username),
    INDEX idx_role (role)
);
```

#### Seed（仅 dev）
- admin/admin123!（首次强制改密）
- ops/ops123!
- boss/boss123!

---

### Module 05: 成本中心

#### 交付
- `CostCenterMapping` entity + repository + service + controller
- Flyway 迁移: `V05__cost_center.sql`

---

### Module 06: 号码基础

#### 交付
- `PhoneNumber` entity + `PhoneHistory` + `PhoneSurrenderRecord`
- PhoneRepository: 列表/搜索/状态过滤
- PhoneService: list/get/history
- PhoneController: GET only 5 个端点
- Flyway 迁移: `V06__phone_number.sql`

#### 表结构（含乐观锁等关键字段）
```sql
CREATE TABLE phone_number (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    user_id VARCHAR(20) NULL,
    extension_number VARCHAR(10) NULL,
    extension_type VARCHAR(20) NULL,
    is_reentry TINYINT(1) NOT NULL DEFAULT 0,
    allocation_org_id BIGINT UNSIGNED NULL,
    status ENUM('idle', 'active', 'stopped', 'cancelled', 'reserved', 'disabled') NOT NULL,
    org_id BIGINT UNSIGNED NULL,
    remark VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (org_id) REFERENCES org_structure(id),
    FOREIGN KEY (allocation_org_id) REFERENCES org_structure(id),
    INDEX idx_status (status),
    INDEX idx_org (org_id),
    INDEX idx_extension (extension_number)
);

CREATE TABLE phone_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id BIGINT UNSIGNED NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NULL,
    from_user VARCHAR(20) NULL,
    to_user VARCHAR(20) NULL,
    from_org VARCHAR(200) NULL,
    to_org VARCHAR(200) NULL,
    work_order_no VARCHAR(50) NULL,
    operator VARCHAR(50) NOT NULL,
    operated_at DATETIME NOT NULL,
    remark VARCHAR(500) NULL,
    FOREIGN KEY (phone_id) REFERENCES phone_number(id),
    INDEX idx_phone (phone_id),
    INDEX idx_operated_at (operated_at DESC)
);

CREATE TABLE phone_surrender_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id BIGINT UNSIGNED NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    final_user VARCHAR(20) NULL,
    final_org VARCHAR(200) NULL,
    surrender_date DATE NOT NULL,
    surrender_type VARCHAR(20) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    work_order_no VARCHAR(50) NULL,
    remark VARCHAR(500) NULL,
    archived_at DATETIME NOT NULL,
    INDEX idx_phone_number (phone_number),
    INDEX idx_surrender_date (surrender_date)
);
```

---

### Module 07: 分机号池

#### 交付
- `ExtensionPool` entity + repository
- ExtensionService: CRUD + 重叠检测 + 用量计算 + 三色预警
- Flyway 迁移: `V07__extension_pool.sql`

---

### Module 08: 区号对照

#### 交付
- `AreaCodeOrgMapping` entity + repository
- CRUD
- Flyway 迁移: `V08__area_code.sql`

---

### Module 09: 号码分配/回收 ⚠️ 并发关键

#### P0 注意
- **悲观锁超时配置：** 必须设置 `jakarta.persistence.lock.timeout=5000`（5秒）
- **Repository 正确写法：** 自定义方法 + `@Lock` + `@QueryHints`
- **双轨回收：** auto→idle / manual→归还号池

#### 交付
- PhoneRepository 专用锁方法
- allocate/reclaim 完整业务逻辑 + history + 通知
- Flyway 迁移: 无新表

---

### Module 10: 号码状态变更

#### 交付
- reserve/release/disable/enable/trouble/surrender
- Flyway 迁移: 无新表

---

### Module 11: 号码变更

#### P0 注意
- **双锁防死锁：** `ORDER BY phone.id ASC` 确保锁顺序一致
- **change-number/change-user/change-org**

---

### Module 12: 号码导入

#### 交付
- @Async + 批量处理 + 进度查询 + 冲突处理
- Flyway 迁移: `V12__import_batch.sql`

---

### Module 13: 话机基础

#### 交付
- `PhoneDevice` + `DevicePhoneMapping` + `PhoneDeviceHistory` entity
- CRUD + MAC 归一化
- Flyway 迁移: `V13__phone_device.sql`

---

### Module 14: 话机操作

#### 交付
- 分配/回收/停用/启用/送修/报废/绑定/解绑
- 离职自动回收联动
- Flyway 迁移: 无新表

---

### Module 15: 通知系统

#### 交付
- `SysNotification` entity
- NotificationService + 完整通知触发矩阵
- Flyway 迁移: `V15__notification.sql`

#### 通知触发矩阵（必须完整覆盖）
- 号码分配/回收
- 拆机二次确认
- 号码 state change
- 号码 change
- 导入完成
- 话机操作
- 号池预警
- 工单操作
- 离职回收
- 二次入库

---

### Module 16: 权限收尾

#### 交付
- 全面审查所有 Controller 权限注解
- scope 精细化裁剪
- Boss/Finance 全系统只读最终确认

---

### Module 17: 仪表盘 & 功能开关

#### 交付
- DashboardService
- FeatureFlagService + `@ConditionalOnFeatureFlag`
- Flyway 迁移: `V17__feature_flag.sql`

---

## 集成与质量门禁

### 每3个模块后触发I4新会话验证

| 里程碑 | 验证内容 |
|--------|----------|
| M01-M03 | 新对话加载 KB+v3，执行回归脚本 |
| M04-M06 | 同上 |
| M07-M09 | 同上 |
| M10-M12 | 同上 |
| M13-M15 | 同上 |
| M16-M17 | 同上 |

### Phase 1 最终门禁

1. **85功能点全量测试**
2. **性能基准**：号码列表<2s, 组织树<1s, 导入100条<5min
3. **并发压测**：allocate 100并发无重复分配 + M11双锁死锁测试 + 号池耗尽测试
4. **安全测试**：SQL注入/XSS/CSRF全部通过

---

## 执行计划时间表

### Phase 1: 基础平台（17个模块）
- **总工期：** 约 18 个工作日
- **每日进度：** 平均 1 个模块，复杂模块（M04/M09/M11/M14）各 2 天

### Phase 2/3: 待 Phase 1 完成后继续

---

## 风险与注意事项

### 关键风险点
1. **技术栈跨度大：** 严格按v3规范重写，不修补现有代码
2. **并发处理：** M09/M11/M14 必须正确实现悲观锁+乐观锁双保险
3. **状态机不一致：** 必须统一使用6种标准状态
4. **权限配置：** 每个Module从M04开始就必须加权限注解

### 禁止事项
- ❌ **React 升级到 19：** 完全禁止
- ❌ **临时跳过门禁：** 必须G1-G5全通过
- ❌ **绕过工单直接操作：** Phase 2 迁移后必须通过工单
- ❌ **使用未定义的 SUPER_ADMIN 角色：** 仅使用 admin/ops/finance/boss

---

## 下一步（立即执行）

1. 确认本计划批准
2. 继续完成 M01 剩余部分
3. 按顺序开发 M02-M17，严格遵循 6步流程
