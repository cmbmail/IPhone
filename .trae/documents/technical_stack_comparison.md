# PhoneBiz 技术栈对比详细分析

> 日期：2026-05-15

---

## 概述

这是一份详细的技术栈对比文档，说明了**现有项目**（位于 `/home/data/apps/phone_ip/data/apps/phonebiz`）与 **v3 金融级开发规范**之间的具体差异。

---

## 1. 后端技术栈对比

| 维度 | 现有项目 | v3 规范要求 | 差异 | 影响 |
|------|---------|------------|------|------|
| **构建工具** | Maven (`pom.xml`) | Gradle (`build.gradle`) | 🔴 完全不同 | 构建系统重写 |
| **Java 版本** | 1.8 (`<java.version>1.8</java.version>`) | 17 LTS | 🔴 跨度太大 | 语法、API 完全升级 |
| **Spring Boot 版本** | 2.7.18 | 3.2.5 | 🔴 跨度太大 | Jakarta EE 迁移 |
| **ORM 框架** | MyBatis Plus 3.5.3.1 | Spring Data JPA | 🔴 完全不同 | 重写所有数据访问层 |
| **安全框架** | 自定义拦截器 + JWT 0.9.1 | Spring Security + JWT 0.12.x | 🔴 完全不同 | 重写安全体系 |
| **数据库迁移** | 手动 SQL (`sql/init.sql`) | Flyway | 🔴 完全不同 | 重写迁移机制 |
| **工具库** | Hutool 5.8.18 | 无特定要求 | 🟡 可选替换 | 可保留但需评估 |
| **代码检查** | 无 | CheckStyle 10.17.0 | 🔴 缺失 | 需要添加 |
| **依赖安全** | 无 | OWASP Dependency Check 9.2.0 | 🔴 缺失 | 需要添加 |
| **Spring Java Format** | 无 | 0.0.43 | 🔴 缺失 | 需要添加 |

### 1.1 现有项目后端关键文件

| 文件 | 位置 | 说明 |
|------|------|------|
| [pom.xml](file:///home/data/apps/phone_ip/data/apps/phonebiz/pom.xml) | Maven 配置文件 | Spring Boot 2.7.18 + MyBatis Plus + Java 8 |
| [PhoneStatus.java](file:///home/data/apps/phone_ip/data/apps/phonebiz/src/main/java/com/cmbchina/phonebiz/enums/PhoneStatus.java) | 状态枚举 | 自定义 5 种状态，与规范完全不兼容 |
| [PhoneNumber.java](file:///home/data/apps/phone_ip/data/apps/phonebiz/src/main/java/com/cmbchina/phonebiz/entity/PhoneNumber.java) | 实体类 | MyBatis Plus 注解，缺少 version 等字段 |

### 1.2 后端代码示例对比

#### 现有项目 - MyBatis Plus 方式
```java
// MyBatis Plus 实体（简化）
@Data
@TableName("phone_number")
public class PhoneNumber extends BaseEntity {
    private String phoneNumber;
    private PhoneStatus status;
    // 无 version 字段！
}

// MyBatis Plus Mapper
public interface PhoneNumberMapper extends BaseMapper<PhoneNumber> {
}
```

#### v3 规范 - JPA 方式
```java
// JPA 实体（简化）
@Data
@Entity
@Table(name = "phone_number")
public class PhoneNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version // 乐观锁！
    private Long version;
    
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private PhoneStatus status; // 6 种标准状态
}

// JPA Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {
}
```

---

## 2. 前端技术栈对比

| 维度 | 现有项目 | v3 规范要求 | 差异 | 影响 |
|------|---------|------------|------|------|
| **类型系统** | JavaScript (`.jsx`) | TypeScript (`.tsx`) | 🔴 完全不同 | 需要迁移到类型安全 |
| **React 版本** | `^18.2.0` (浮动版本) | `18.3.1` (精确锁定) | 🟡 版本不同 | 可升级但需精确锁定 |
| **Antd 版本** | `^5.12.0` (浮动版本) | `5.22.3` (精确锁定) | 🟡 版本不同 | 可升级但需精确锁定 |
| **Vite 版本** | `^5.0.0` (浮动版本) | `5.4.11` (精确锁定) | 🟡 版本不同 | 可升级但需精确锁定 |
| **状态管理** | 无 | Zustand 5.0.1 | 🔴 缺失 | 需要添加 |
| **服务端状态** | 无 | @tanstack/react-query 5.62.0 | 🔴 缺失 | 需要添加 |
| **代码格式化** | 无 | Prettier 3.4.2 | 🔴 缺失 | 需要添加 |
| **静态检查** | 无 | ESLint 9.15.0 | 🔴 缺失 | 需要添加 |

### 2.1 现有项目前端关键文件

| 文件 | 位置 | 说明 |
|------|------|------|
| [package.json](file:///home/data/apps/phone_ip/data/apps/phonebiz/frontend/package.json) | 前端配置 | JavaScript 版本，无 Zustand、React Query、Prettier、ESLint |
| [App.jsx](file:///home/data/apps/phone_ip/data/apps/phonebiz/frontend/src/App.jsx) | 主组件 | JavaScript 写法 |

---

## 3. 状态机对比（关键差异）

### 3.1 现有项目状态机
```java
// 现有项目：自定义 5 种状态
public enum PhoneStatus {
    UNASSIGNED("未分配"),
    ASSIGNED("已分配"),
    IN_USE("使用中"),
    SUSPENDED("已停用"),
    RECYCLED("已回收");
}
```

### 3.2 v3 规范状态机
```java
// v3 规范：6 种标准状态
public enum PhoneStatus {
    IDLE("空闲"),
    ACTIVE("使用中"),
    STOPPED("停用"),
    CANCELLED("拆机"),
    RESERVED("预留"), // 现有项目缺失！
    DISABLED("禁用");  // 现有项目缺失！
}
```

| 维度 | 现有项目 | v3 规范 | 差异 |
|------|---------|--------|------|
| 状态数量 | 5 种 | 6 种 | 🔴 缺少预留/禁用状态 |
| 状态名称 | 中文语义英文 | 国际标准英文 | 🔴 命名完全不同 |
| 状态机逻辑 | 自定义转换规则 | 标准转换规则 | 🔴 业务逻辑完全不兼容 |

---

## 4. 数据库设计对比

### 4.1 现有项目表结构
从 `sql/init.sql` 可以看出：
- 约 8 张表
- `phone_number` 表缺少 `version`、`extension_type`、`is_reentry`、`allocation_org_id` 等关键字段
- 无 Flyway 迁移机制

### 4.2 v3 规范表结构（完整 24 张表）
包括（但不限于）：
| 表名 | 说明 |
|------|------|
| `org_structure` | 组织架构 |
| `employee` | 员工 |
| `phone_number` | 电话号码（完整字段，包括乐观锁） |
| `extension_pool` | 分机号池 |
| `area_code_org_mapping` | 区号组织映射 |
| `phone_history` | 号码历史（永久保留） |
| `phone_surrender_record` | 拆机记录 |
| `phone_device` | 话机管理（现有项目缺失！） |
| `device_phone_mapping` | 话机号码映射（现有项目缺失！） |
| `phone_device_history` | 话机历史（现有项目缺失！） |
| `import_batch` | 导入批次（现有项目缺失！） |
| `sys_feature_flag` | 功能开关（现有项目缺失！） |
| `sys_feature_flag_log` | 开关日志（现有项目缺失！） |
| `cost_center_mapping` | 成本中心 |
| `work_order` | 工单 |
| `work_order_item` | 工单项 |
| `phone_snapshot` | 月度快照 |
| `bill_raw` | 账单原始数据 |
| `bill_allocation` | 账单分摊 |
| `subsidiary_reconciliation` | 子公司对账 |
| `invoice` | 发票 |
| `invoice_file` | 发票文件 |
| `invoice_distribution` | 发票分发 |
| `sys_user` | 系统用户 |
| `sys_notification` | 系统通知 |

---

## 5. 业务功能对比

### 5.1 现有项目已实现功能
✅ 基础组织架构  
✅ 基础员工管理  
✅ 基础权限角色  
✅ 登录认证（JWT）  
✅ 电话号码基础 CRUD  
✅ 工单基础框架（不完整）  

### 5.2 v3 规范 Phase 1 完整功能（现有项目缺失的）
🔴 分机号池管理  
🔴 区号自动匹配  
🔴 号码分配/回收（并发悲观锁）  
🔴 号码状态管理（预留/禁用/停用/拆机）  
🔴 号码变更（改人/改号/改组织，双锁防死锁）  
🔴 Excel 批量导入  
🔴 话机完整管理（分配/回收/送修/报废/绑定）  
🔴 通知系统（通知触发矩阵）  
🔴 系统功能开关（灰度发布）  
🔴 仪表盘  
🔴 离职自动回收  
🔴 成本中心管理  

---

## 6. 总结：为什么说技术栈完全不匹配？

### 6.1 后端不匹配的核心原因
1. **Spring Boot 版本跨度太大**：2.7 → 3.2，意味着：
   - `javax.*` → `jakarta.*` 完整包名迁移
   - 众多 API 变更和移除
2. **ORM 框架完全不同**：MyBatis Plus vs JPA，需要重写所有数据访问层
3. **Java 版本差距**：8 → 17，大量新特性和语言特性可用，但也意味着旧代码必须重写
4. **安全体系完全重写**：自定义拦截器 → Spring Security
5. **数据库迁移机制重写**：手动 SQL → Flyway
6. **缺少金融级工具链**：CheckStyle、OWASP、Spring Java Format

### 6.2 前端不匹配的核心原因
1. **JavaScript → TypeScript 迁移**：类型系统完全不同
2. **缺少必要的状态管理**：Zustand（客户端状态）、React Query（服务端状态）
3. **缺少代码规范工具**：Prettier、ESLint
4. **版本没有精确锁定**：现有项目使用 `^` 浮动版本，v3 规范要求精确锁定

---

## 7. 建议方案

| 方案 | 优点 | 缺点 |
|------|------|------|
| **方案 A：完全按 v3 规范重写** | 符合金融级标准，技术栈现代化，可扩展性强 | 需要时间重写 |
| **方案 B：在现有项目基础上修补** | 保留现有代码，快速上线 | 技术栈不一致，后续维护成本高，不符合规范 |
| **方案 C：分阶段迁移** | 逐步改进 | 需要长期规划，时间成本高 |

**推荐方案 A：完全按 v3 规范重写**，因为：
1. 金融系统对安全、可审计、可维护性要求极高
2. 现有代码与规范差异太大，修补成本甚至可能高于重写
3. v3 规范已经提供了完整的模块开发计划，可按步执行

---

## 8. 文件位置汇总

### 现有项目文件
- [pom.xml](file:///home/data/apps/phone_ip/data/apps/phonebiz/pom.xml)
- [frontend/package.json](file:///home/data/apps/phone_ip/data/apps/phonebiz/frontend/package.json)
- [PhoneStatus.java](file:///home/data/apps/phone_ip/data/apps/phonebiz/src/main/java/com/cmbchina/phonebiz/enums/PhoneStatus.java)
- 完整项目：`/home/data/apps/phone_ip/data/apps/phonebiz`

### v3 规范文件
- [PhoneBiz-金融级开发规范_v3.md](file:///home/data/apps/phone_ip/20260510需求/PhoneBiz-金融级开发规范_v3.md)
- [PhoneBiz-Phase1-功能清单.md](file:///home/data/apps/phone_ip/20260510需求/PhoneBiz-Phase1-功能清单.md)
- [PhoneBiz-KNOWLEDGE_BASE.md](file:///home/data/apps/phone_ip/20260510需求/PhoneBiz-KNOWLEDGE_BASE.md)
- [PhoneBiz数据库设计_DDL.sql](file:///home/data/apps/phone_ip/20260510需求/PhoneBiz数据库设计_DDL.sql)

### 新创建的项目文件
- [backend/build.gradle](file:///home/data/apps/phone_ip/backend/build.gradle)
- [frontend/package.json](file:///home/data/apps/phone_ip/frontend/package.json)
- [.editorconfig](file:///home/data/apps/phone_ip/.editorconfig)
- [.gitignore](file:///home/data/apps/phone_ip/.gitignore)
