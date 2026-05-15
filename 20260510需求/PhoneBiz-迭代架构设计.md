# PhoneBiz — 系统迭代架构设计

> Layer 1+2+3 | 2026-05-10 | Phase 1 落地 Phase 2/3 受益

---

## Layer 1：Flyway 数据库版本化

### 变更

`build.gradle` 加依赖：

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

`application.yml` 配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 文件结构

```
backend/src/main/resources/db/migration/
├── V1__phase1_core.sql         # Phase 1 全部 19 张表
├── V2__phase2_workorder.sql    # Phase 2 工单系统（未来）
├── V3__phase3_billing.sql      # Phase 3 账单+发票（未来）
```

原 `PhoneBiz数据库设计_DDL.sql` 拆分为 `V1__phase1_core.sql`。

### 迭代流程

```
Phase 2 发布时：
1. 写 V2__phase2_workorder.sql（CREATE TABLE work_order 等）
2. 部署 → Flyway 自动执行新增脚本
3. 现有 Phase 1 数据不受影响
```

### 新增任务

| 任务 | 说明 |
|------|------|
| T-005a | Flyway 依赖配置 + application.yml |
| T-005b | DDL 迁移到 V1__phase1_core.sql |

---

## Layer 2：模块化包结构

### 包结构

```
com.phonebiz
├── PhoneBizApplication.java
├── common/                          # 跨模块共享
│   ├── ApiResponse.java
│   ├── PageResult.java
│   ├── ResultCode.java
│   ├── BaseEntity.java
│   └── ErrorCode.java（24个枚举）
├── config/                          # 全局配置
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── JwtConfig.java
│   ├── FlywayConfig.java
│   └── DataInitializer.java
├── security/                        # 认证鉴权
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── PermissionEvaluator.java
├── exception/                       # 异常处理
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── module/
│   ├── auth/                        # 登录/登出/改密
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   └── dto/{LoginRequest,LoginResponse,UserInfoResponse}.java
│   ├── org/                         # 组织架构
│   │   ├── controller/OrgController.java
│   │   ├── service/OrgService.java
│   │   ├── repository/OrgRepository.java
│   │   ├── entity/OrgStructure.java
│   │   └── dto/
│   ├── employee/                    # 员工
│   │   ├── controller/...
│   │   ├── service/...
│   │   └── ...
│   ├── phone/                       # 电话号码
│   │   ├── controller/
│   │   ├── service/
│   │   ├── service/impl/           # allocate / reclaim / trouble / surrender...
│   │   ├── repository/
│   │   ├── entity/{PhoneNumber,PhoneHistory,PhoneSurrenderRecord}.java
│   │   └── dto/
│   ├── device/                      # 🆕 电话机
│   │   ├── controller/DeviceController.java
│   │   ├── service/DeviceService.java
│   │   ├── repository/
│   │   ├── entity/{PhoneDevice,DevicePhoneMapping,PhoneDeviceHistory}.java
│   │   └── dto/
│   ├── pool/                        # 分机号池
│   │   └── ...
│   ├── areacode/                    # 区号匹配
│   │   └── ...
│   ├── user/                        # 用户管理
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── repository/SysUserRepository.java
│   │   └── entity/SysUser.java
│   ├── notification/                # 通知
│   │   ├── service/NotificationService.java
│   │   ├── repository/SysNotificationRepository.java
│   │   └── entity/SysNotification.java
│   ├── importx/                     # Excel 导入
│   │   ├── controller/ImportController.java
│   │   ├── service/ImportService.java
│   │   └── entity/ImportBatch.java
│   └── feature/                     # 🆕 功能开关
│       ├── entity/SysFeatureFlag.java
│       ├── repository/FeatureFlagRepository.java
│       └── service/FeatureFlagService.java
└── util/
    ├── PhoneNumberGenerator.java
    ├── TreeBuilder.java
    └── MacNormalizer.java
```

### 迭代规则

Phase 2 新增工单模块时：

```
com.phonebiz.module.workorder/       ← 新包，不碰 Phase1 代码
├── controller/WorkOrderController.java
├── service/WorkOrderService.java
├── entity/{WorkOrder,WorkOrderItem}.java
└── dto/
```

### 新增任务

| 任务 | 说明 |
|------|------|
| T-001a | 按 module/ 子包重排项目骨架 |

---

## Layer 3：功能开关

### DDL

```sql
CREATE TABLE sys_feature_flag (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    flag_key        VARCHAR(50) NOT NULL COMMENT '开关Key（如 device_mgmt_enabled）',
    flag_name       VARCHAR(100) NOT NULL COMMENT '中文名称',
    enabled         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description     VARCHAR(500) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flag_key (flag_key)
) COMMENT='系统功能开关表';

-- 种子数据
INSERT INTO sys_feature_flag (flag_key, flag_name, enabled) VALUES
('phone_lifecycle', '号码生命周期管理', 1),
('device_mgmt', '电话机管理', 1),
('extension_pool', '分机号池', 1),
('excel_import', 'Excel导入', 1),
('area_code_match', '区号匹配', 1),
('work_order', '工单系统', 0),     -- Phase 2
('billing', '账单管理', 0),         -- Phase 3
('invoice', '发票分发', 0);         -- Phase 3
```

### 使用方式

```java
// 后端：Controller 层加开关保护
@RestController
@RequestMapping("/api/devices")
@ConditionalOnFeatureFlag("device_mgmt")    // 关掉后 404
public class DeviceController { ... }

// 前端：菜单渲染时过滤
const flags = await getFeatureFlags();
if (flags.device_mgmt) {
    menuItems.push({ key: 'devices', label: '电话机管理' });
}
```

### @ConditionalOnFeatureFlag 实现

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(FeatureFlagCondition.class)
public @interface ConditionalOnFeatureFlag {
    String value();  // flag_key
}

public class FeatureFlagCondition implements Condition {
    @Override
    public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata meta) {
        String flagKey = (String) meta.getAnnotationAttributes(...).get("value");
        return featureFlagService.isEnabled(flagKey);
    }
}
```

### 用途约定

| 约束 | 说明 |
|------|------|
| 仅模块级 | 不对单个 API 加开关（避免散弹式 if/else） |
| 默认启用 | Phase 1 所有核心模块 flag 默认 enabled=1 |
| Phase 2/3 预留 | work_order / billing / invoice 预置为 0 |
| 灰度场景 | 子公司A试点工单 → 在代码侧做 flag + orgId 组合判断 |

### 新增任务

| 任务 | 说明 |
|------|------|
| T-005d | sys_feature_flag 表 DDL + 种子数据 |
| T-005e | @ConditionalOnFeatureFlag 注解 + FeatureFlagCondition |
| T-005f | FeatureFlagService + 前端 flags API |

---

## 变更汇总

| Layer | 新增表 | 新增任务 | 成本 |
|:--:|------|:--:|------|
| 1 Flyway | — | 2 | 极低 |
| 2 模块化 | — | 1 | 低（Phase1 未开工，改包结构零成本） |
| 3 功能开关 | sys_feature_flag | 3 | 低（1表+1注解+1Service） |
| **合计** | **1 张表** | **6 个任务** | **约 0.5 天** |
