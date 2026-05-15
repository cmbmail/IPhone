# PhoneBiz 项目重构计划

> 日期：2026-05-15 | 基于现有代码与设计文档对比分析

---

## 一、差异对比总览

### 1.1 技术栈差异

| 维度 | 设计文档要求 | 当前实现 | 差距评价 |
|-----|------------|---------|---------|
| **后端框架** | Spring Boot 3.2.x | Spring Boot 2.7.18 | 🔴 需大版本升级 |
| **Java 版本** | Java 17 | Java 8 | 🔴 需升级 |
| **ORM框架** | Spring Data JPA | MyBatis Plus 3.5.3.1 | 🔴 需完全迁移 |
| **构建工具** | Gradle | Maven | 🟡 可选择迁移 |
| **数据库迁移** | Flyway | 手动SQL脚本 | 🟡 建议添加 |
| **安全框架** | Spring Security + JWT | 自定义拦截器 + JWT | 🔴 需重构 |
| **前端语言** | TypeScript | JavaScript | 🟡 建议迁移 |
| **前端状态管理** | Zustand + React Query | 无 | 🔴 需添加 |

### 1.2 数据库设计差异

| 设计文档表 | 当前实现 | 状态 |
|---------|---------|------|
| `org_structure` | `sys_org` | 🟡 结构不同需重构 |
| `employee` | `sys_employee` | 🟡 结构不同需重构 |
| `sys_user` | ❌ 无独立表 | 🔴 需新增 |
| `phone_number` | `phone_number` | 🟡 字段不足需补充 |
| `phone_history` | ❌ 缺失 | 🔴 需新增 |
| `phone_surrender_record` | ❌ 缺失 | 🔴 需新增 |
| `extension_pool` | ❌ 缺失 | 🔴 需新增 |
| `area_code_org_mapping` | ❌ 缺失 | 🔴 需新增 |
| `phone_device` | ❌ 缺失 | 🔴 Phase 1后期 |
| `device_phone_mapping` | ❌ 缺失 | 🔴 Phase 1后期 |
| `phone_device_history` | ❌ 缺失 | 🔴 Phase 1后期 |
| `sys_notification` | ❌ 缺失 | 🔴 需新增 |
| `sys_feature_flag` | ❌ 缺失 | 🟢 Phase 1后期 |
| `work_order` | `work_order` | 🟡 需补充字段 |
| `work_order_item` | ❌ 缺失 | 🔴 Phase 2 |
| `phone_snapshot` | ❌ 缺失 | 🔴 Phase 2后期 |
| `cost_center_mapping` | ❌ 缺失 | 🔴 Phase 2后期 |
| `bill_raw` | ❌ 缺失 | 🔴 Phase 3 |
| `bill_allocation` | ❌ 缺失 | 🔴 Phase 3 |
| `invoice` | ❌ 缺失 | 🔴 Phase 3 |
| `invoice_file` | ❌ 缺失 | 🔴 Phase 3 |
| `invoice_distribution` | ❌ 缺失 | 🔴 Phase 3 |
| `subsidiary_reconciliation` | ❌ 缺失 | 🔴 Phase 3 |

### 1.3 状态枚举差异

**设计文档状态（6种）：**
- `idle` - 空闲
- `active` - 使用中
- `stopped` - 停机
- `cancelled` - 已拆机
- `reserved` - 预留
- `disabled` - 禁用

**当前实现状态（7种）：**
- `UNASSIGNED` - 未分配
- `ASSIGNED` - 已分配
- `IN_USE` - 使用中
- `SUSPENDED` - 暂停
- `RECYCLED` - 回收
- 其他...

**评价：** 🔴 状态体系完全不同，需全部重构

### 1.4 业务功能完成度对比

| 模块 | 设计文档要求 | 当前实现 | 完成度 |
|-----|------------|---------|-------|
| **认证与鉴权** | 登录/锁定/Token/权限/强制改密 | 基本登录 + JWT | ~20% |
| **组织架构** | 树/CRUD/path/level/scope裁剪 | 基本CRUD | ~30% |
| **员工管理** | CRUD/工号规则/自动建用户/离职回收 | 基本CRUD | ~25% |
| **号码基础** | 列表/详情/历史/归档 | 基本列表 | ~15% |
| **号码操作** | 7种操作/并发锁/通知 | 基本状态切换 | ~10% |
| **号池管理** | CRUD/重叠检测/自动分配 | ❌ 无 | 0% |
| **区号匹配** | CRUD/自动匹配 | ❌ 无 | 0% |
| **Excel导入** | 异步/预览/确认 | ❌ 无 | 0% |
| **话机管理** | CRUD/分配/解绑 | ❌ 无 | 0% |
| **通知系统** | 推送/列表/未读数 | ❌ 无 | 0% |
| **Dashboard** | 统计/功能开关 | ❌ 无 | 0% |

**总体完成度：** ~12%

---

## 二、重构策略决策

### 2.1 方案选择

**方案A：完全按设计文档重写（推荐）**
- ✅ 符合金融级规范
- ✅ 避免技术债务
- ✅ 一次性解决所有问题
- ❌ 工作量较大

**方案B：在现有基础上逐步完善**
- ✅ 利用已有代码
- ❌ 技术栈不一致
- ❌ 持续产生债务
- ❌ 维护成本高

**决策：采用方案A - 完全按设计文档重构**

### 2.2 重构阶段划分

```
Phase 0: 技术栈升级（3天）
  ├─ Java 8 → 17
  ├─ Spring Boot 2 → 3
  ├─ MyBatis Plus → JPA
  └─ Maven → Gradle

Phase 1: 基础平台重构（15工作日）
  ├─ M01: 项目骨架
  ├─ M02: 组织架构
  ├─ M03: 员工管理
  ├─ M04: 认证鉴权
  ├─ M05: 成本中心（可选）
  ├─ M06: 号码基础
  ├─ M07: 分机号池
  ├─ M08: 区号匹配
  ├─ M09: 号码操作
  ├─ M10: 状态变更
  ├─ M11: 号码变更
  ├─ M12: Excel导入
  ├─ M13: 话机基础
  ├─ M14: 话机操作
  ├─ M15: 通知系统
  ├─ M16: 权限收尾
  └─ M17: Dashboard

Phase 2/3: 后续迭代（按计划进行）
```

---

## 三、技术栈升级详细方案（Phase 0）

### 3.1 Java 8 → 17 升级

**改动点：**
- pom.xml 中 java.version 从 8 → 17
- Spring Boot 3.x 只支持 Java 17+

**验证命令：**
```bash
java -version  # 确保是 17+
```

### 3.2 Spring Boot 2.7.18 → 3.2.x 升级

**关键变更：**
1. `javax.*` → `jakarta.*` 包名迁移
2. Spring Security 配置方式变更
3. Thymeleaf/Jackson 等依赖版本升级

### 3.3 MyBatis Plus → Spring Data JPA 迁移

**迁移步骤：**
1. 保留现有代码到 `legacy/` 目录（参考用途）
2. 按设计文档重新定义 Entity（使用 JPA 注解）
3. 定义 Repository 接口
4. 重写 Service 层
5. 调整 Controller 层

**Entity 示例对比：**

当前（MyBatis Plus）：
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("phone_number")
public class PhoneNumber extends BaseEntity {
    private String phoneNumber;
    private String status;
    private Long employeeId;
    private String employeeName;
    private String remark;
}
```

目标（JPA）：
```java
@Entity
@Table(name = "phone_number")
@Data
public class PhoneNumber extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PhoneStatus status;

    @Column(name = "user_id")
    private String userId;  // 工号，非employee表id

    @Column(name = "extension_number")
    private String extensionNumber;

    @Column(name = "extension_type")
    private ExtensionType extensionType;

    @Column(name = "org_id")
    private Long orgId;

    @Version  // 乐观锁
    @Column(name = "version")
    private Long version;

    // ... 其他字段
}
```

### 3.4 Maven → Gradle 迁移

**build.gradle 配置参考：**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.cmbchina'
version = '1.0.0-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.mysql:mysql-connector-j:8.0.36'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.withType(Test) {
    useJUnitPlatform()
}
```

---

## 四、数据库重构方案（Phase 0 后期）

### 4.1 完整表结构设计（按设计文档）

**组织架构表 `org_structure`：**
```sql
CREATE TABLE org_structure (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'group/subsidiary/dept',
    level INT NOT NULL,
    path VARCHAR(500) NOT NULL COMMENT 'e.g., /1/5/',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**员工表 `employee`：**
```sql
CREATE TABLE employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_no VARCHAR(20) UNIQUE NOT NULL COMMENT '6位',
    name VARCHAR(50) NOT NULL,
    org_id BIGINT NOT NULL,
    position VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    entry_date DATE,
    leave_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    is_virtual BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_org_id (org_id),
    INDEX idx_employee_no (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**系统用户表 `sys_user`：**
```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    employee_no VARCHAR(20),
    role VARCHAR(20) NOT NULL COMMENT 'admin/ops/finance/boss',
    scope_org_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    password_changed_at DATETIME,
    login_fail_count INT DEFAULT 0,
    locked_until DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_employee_no (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**电话号码表 `phone_number`（完整版）：**
```sql
CREATE TABLE phone_number (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    user_id VARCHAR(20) COMMENT 'employee_no',
    extension_number VARCHAR(20),
    extension_type VARCHAR(20) COMMENT 'auto/manual',
    is_shared BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'idle',
    org_id BIGINT,
    allocation_org_id BIGINT COMMENT '分机号分配来源组织',
    is_reentry BOOLEAN DEFAULT FALSE,
    remark VARCHAR(500),
    version BIGINT DEFAULT 0 COMMENT '乐观锁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_status (status),
    INDEX idx_org_id (org_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**号码历史表 `phone_history`：**
```sql
CREATE TABLE phone_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    from_user VARCHAR(20),
    to_user VARCHAR(20),
    from_org_id BIGINT,
    to_org_id BIGINT,
    work_order_no VARCHAR(50),
    reason VARCHAR(500),
    operator VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_id (phone_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**拆机归档表 `phone_surrender_record`：**
```sql
CREATE TABLE phone_surrender_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    final_user VARCHAR(20),
    final_org_id BIGINT,
    surrender_date DATETIME NOT NULL,
    surrender_type VARCHAR(50),
    reason VARCHAR(500),
    operator VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_number (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**分机号池表 `extension_pool`：**
```sql
CREATE TABLE extension_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    start_number VARCHAR(20) NOT NULL COMMENT 'e.g., 100000',
    end_number VARCHAR(20) NOT NULL COMMENT 'e.g., 199999',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_org_id (org_id),
    UNIQUE KEY uk_org_range (org_id, start_number, end_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**区号匹配表 `area_code_org_mapping`：**
```sql
CREATE TABLE area_code_org_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area_code VARCHAR(10) NOT NULL COMMENT 'e.g., 010, 021',
    org_id BIGINT NOT NULL,
    priority INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_area_code (area_code),
    INDEX idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**通知表 `sys_notification`：**
```sql
CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    target_user VARCHAR(50) NOT NULL,
    related_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target_user (target_user),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 迁移策略

1. **保留现有数据库**：重命名为 `phonebiz_legacy`
2. **创建新数据库**：`phonebiz_v2`，执行新DDL
3. **数据迁移脚本**：编写 SQL 从旧表迁移数据到新表

---

## 五、Phase 1 业务功能重构详细计划

### 阶段里程碑

| 阶段 | 工作日 | 交付物 |
|-----|-------|-------|
| Phase 0 | 3天 | 技术栈升级完成 |
| Step 1-2 | 4天 | 骨架+认证 |
| Step 3-4 | 5天 | 组织员工+号码查询 |
| Step 5-6 | 5天 | 号码操作+号池导入 |
| Step 7 | 1天 | Dashboard+部署 |
| **总计** | **18天** | **完整PhoneBiz v1.0** |

### 详细任务清单

#### M01: 项目骨架（1天）

**后端任务：**
- [ ] Gradle 项目配置
- [ ] application.yml 配置（MySQL + Flyway）
- [ ] `ApiResponse<T>`、`PageResult<T>`、`ResultCode` 枚举
- [ ] `BaseEntity` 基类
- [ ] `BusinessException`、`ErrorCode`、`GlobalExceptionHandler`
- [ ] `AuditLogger`、`NotificationPublisher` 空接口
- [ ] Flyway 基线 SQL（空迁移）
- [ ] CheckStyle + OWASP 配置
- [ ] `.editorconfig`、`.gitignore`

**前端任务：**
- [ ] Vite + React 18 + TypeScript 配置
- [ ] `package.json` 依赖版本锁定（按设计文档）
- [ ] `types/index.ts` 全量类型定义
- [ ] `api/client.ts` axios 实例 + 拦截器
- [ ] App 布局骨架（Sidebar + Header）
- [ ] LoginPage 骨架
- [ ] authStore 骨架

**验证门禁：**
- `./gradlew bootRun` 启动成功
- `npm run dev` 无编译错误
- 浏览器能看到登录页

---

#### M02: 组织架构（1.5天）

**后端任务：**
- [ ] `OrgStructure` Entity + Repository
- [ ] `OrgService`（CRUD + TreeBuilder + path/level 自动计算）
- [ ] `OrgController`（5个端点）
- [ ] 同名检测 + 深度限制（≤25层）
- [ ] `PermissionEvaluator` 基础实现（scope裁剪）
- [ ] 种子数据：集团总部 + 待分配组织

**前端任务：**
- [ ] `OrgTree` 组件（Antd Tree）
- [ ] `OrgFormDialog`（新增/编辑）
- [ ] `OrgPage` 页面

**验证门禁：**
- 组织树展示正确
- 新增组织 path/level 自动计算
- 同名组织拒绝

---

#### M03: 员工管理（1.5天）

**后端任务：**
- [ ] `Employee` Entity + Repository
- [ ] `EmployeeService`（CRUD + 工号校验）
- [ ] `EmployeeController`
- [ ] 工号规则：6位、纯数字自动分机号、含字母需号池
- [ ] 自动创建 `SysUser`（默认密码）
- [ ] 种子数据：虚拟员工

**前端任务：**
- [ ] `EmployeeTable`（ProTable + 搜索 + 过滤）
- [ ] `EmployeeFormDialog`
- [ ] `EmployeePage`

**验证门禁：**
- 工号校验生效
- 员工列表分页正常
- 创建员工自动创建用户

---

#### M04: 认证鉴权（2天）

**后端任务：**
- [ ] `SysUser` Entity + Repository
- [ ] `JwtTokenProvider`（生成/解析/过期）
- [ ] `SecurityConfig` + `JwtAuthenticationFilter`
- [ ] `UserDetailsServiceImpl`
- [ ] `AuthController` + `AuthService`（login/logout/me/changePassword）
- [ ] 登录锁定逻辑（5次失败 → 锁定30分钟）
- [ ] 首次登录强制改密逻辑
- [ ] 角色常量：`ADMIN`/`OPS`/`FINANCE`/`BOSS`
- [ ] 种子数据：admin/ops/boss 账号

**前端任务：**
- [ ] `LoginForm` 组件
- [ ] authStore（zustand）完整实现
- [ ] `api/auth.ts`（login/logout/me）
- [ ] 路由守卫（token过期跳转登录）
- [ ] Sidebar 菜单按角色显示
- [ ] 首次登录强制改密页

**验证门禁：**
- 登录成功返回 Token
- 5次失败锁定生效
- 路由守卫正常

---

#### M05-M08: 号码基础 + 号池 + 区号（3天）

**后端任务：**
- [ ] `PhoneNumber`、`PhoneHistory`、`PhoneSurrenderRecord` Entity
- [ ] `PhoneRepository`（自定义查询）
- [ ] `PhoneService`（list/get/history）
- [ ] `PhoneController`（GET端点）
- [ ] `ExtensionPool` Entity + Repository + Service + Controller
- [ ] 号池重叠检测
- [ ] `AreaCodeOrgMapping` Entity + Repository + Service + Controller

**前端任务：**
- [ ] `PhoneTable`（分页 + 6状态过滤 + 搜索）
- [ ] `PhoneDetail`（详情 + 历史时间线）
- [ ] `PhoneStatusBadge`（6种颜色）
- [ ] `ExtensionPoolPage`
- [ ] `AreaCodePage`

**验证门禁：**
- 号码列表过滤正确
- 历史时间线展示正常

---

#### M09-M11: 号码核心操作（4天）⚠️ 最复杂

**后端任务：**
- [ ] `PhoneRepository` 自定义锁方法（`findByIdForUpdate`）
- [ ] `allocate`（完整校验链 + 悲观锁 + 分机号双轨）
- [ ] `reclaim`（双轨回收）
- [ ] `trouble`（停机/复机）
- [ ] `surrender`（归档 + 不可逆）
- [ ] `changeUser`、`changeNumber`、`changeOrg`
- [ ] `reserve/release`、`disable/enable`
- [ ] 离职自动回收联动
- [ ] 调用 `NotificationPublisher` 发送通知
- [ ] application.yml 配置锁超时（5秒）

**前端任务：**
- [ ] `PhoneAllocateDialog`
- [ ] `PhoneReclaimDialog`
- [ ] `PhoneTroubleDialog`
- [ ] `PhoneSurrenderDialog`（二次确认）
- [ ] `PhoneChangeUserDialog`
- [ ] `PhoneChangeNumberDialog`
- [ ] `PhoneChangeOrgDialog`
- [ ] `PhoneReserveDialog`
- [ ] `PhoneDisableDialog`
- [ ] 按钮可见性按状态控制

**验证门禁：**
- 所有7种操作正常
- 并发测试通过（无重复分配）

---

#### M12: Excel导入（1天）

**后端任务：**
- [ ] `ImportBatch` Entity + Repository
- [ ] `ImportService`（@Async + 批量处理 + 进度查询）
- [ ] 线程池配置
- [ ] Excel 解析 + 归一化 + 区号匹配 + 判重
- [ ] 预览 + 确认 + 批量提交

**前端任务：**
- [ ] `ImportPage`（上传 → 进度 → 预览 → 确认）

**验证门禁：**
- 100行导入 < 30秒

---

#### M13-M14: 话机管理（2天）

**后端任务：**
- [ ] `PhoneDevice`、`DevicePhoneMapping`、`PhoneDeviceHistory` Entity
- [ ] `DeviceService`（CRUD + 分配/回收/送修/报废）
- [ ] 送修/报废自动解绑号码
- [ ] `DeviceController`（16个端点）

**前端任务：**
- [ ] `DeviceTable`
- [ ] `DeviceDetail`
- [ ] 各类操作 Dialog

**验证门禁：**
- 话机分配/回收正常
- 送修自动解绑

---

#### M15-M17: 通知 + 权限 + Dashboard（2天）

**后端任务：**
- [ ] `SysNotification` Entity + Repository + Service
- [ ] 完整通知触发矩阵
- [ ] 全局 @PreAuthorize 审计
- [ ] `DashboardService`（统计 API）
- [ ] `SysFeatureFlag` Entity + Service
- [ ] docker-compose.yml

**前端任务：**
- [ ] `NotificationList` + 顶部未读数 Badge
- [ ] 前端按钮权限控制
- [ ] `DashboardPage`
- [ ] Swagger UI 集成

**验证门禁：**
- 操作触发通知正确送达
- 权限控制生效
- Dashboard 统计正确

---

## 六、前端重构详细方案

### 6.1 目录结构调整

```
frontend/src/
├── api/
│   ├── client.ts          # axios实例 + 拦截器
│   └── modules/
│       ├── auth.ts
│       ├── org.ts
│       ├── employee.ts
│       ├── phone.ts
│       └── ...
├── components/
│   ├── layout/
│   │   ├── AppLayout.tsx
│   │   ├── Sidebar.tsx
│   │   └── Header.tsx
│   ├── common/
│   │   ├── Loading.tsx
│   │   ├── Empty.tsx
│   │   └── ConfirmDialog.tsx
│   └── phone/
│       ├── PhoneTable.tsx
│       ├── PhoneDetail.tsx
│       └── dialogs/
├── pages/
│   ├── LoginPage.tsx
│   ├── Dashboard.tsx
│   ├── OrgPage.tsx
│   ├── EmployeePage.tsx
│   ├── PhonePage.tsx
│   └── ...
├── stores/
│   ├── authStore.ts
│   └── uiStore.ts
├── types/
│   └── index.ts
├── utils/
│   ├── auth.ts
│   └── tree.ts
├── App.tsx
└── main.tsx
```

### 6.2 核心依赖添加

```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.22.0",
    "antd": "^5.15.0",
    "@ant-design/icons": "^5.3.0",
    "axios": "^1.6.7",
    "zustand": "^5.0.0",
    "@tanstack/react-query": "^5.28.0",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@types/react": "^18.2.60",
    "@types/react-dom": "^18.2.19",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.4.0",
    "vite": "^5.1.6",
    "eslint": "^8.57.0",
    "prettier": "^3.2.5"
  }
}
```

---

## 七、风险与缓解措施

| 风险 | 影响 | 概率 | 缓解措施 |
|-----|------|------|---------|
| Java 17 兼容性问题 | 阻塞 | 中 | 提前在本地测试验证 |
| JPA 锁机制实现错误 | 数据不一致 | 高 | 并发测试强制验证 |
| 分机号双轨逻辑混乱 | 业务错误 | 中 | 单元测试覆盖8种组合 |
| 现有数据丢失 | 严重 | 低 | 保留旧库，迁移脚本双写验证 |
| 开发周期延长 | 交付延迟 | 中 | 按阶段交付，每阶段门禁验证 |

---

## 八、关键验证门禁汇总

每阶段完成后必须通过：

| 阶段 | 验证项 |
|-----|-------|
| M01 | 后端启动成功 + 前端编译通过 |
| M02-M04 | 登录正常 + 组织员工CRUD + 权限裁剪 |
| M05-M08 | 号码列表/详情/历史正常 |
| M09-M11 | **并发测试V7-V9通过** + 所有操作正常 |
| M12 | Excel导入100行成功 |
| M13-M14 | 话机管理正常 |
| M15-M17 | 通知权限Dashboard正常 + docker-compose启动 |

---

**文档结束**

*注：本计划基于设计文档 v5.0 + 现有代码分析制定。*
