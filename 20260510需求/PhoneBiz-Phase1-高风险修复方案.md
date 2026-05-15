# PhoneBiz Phase 1 — 高风险修复方案

> 基于：项目拆解与验证方案 R1-R5 | 日期：2026-05-10
> 方式：逐项 → 根因分析 → 修复设计 → 落实到需求 & 架构

---

## R1：并发分配冲突 — FOR UPDATE 未正确覆盖

### 根因

JPA 默认 `findById()` 不生成 `SELECT ... FOR UPDATE`。如果用 `findById` 查号码再做 `save`，两个线程可能读到同一个 status=idle，都判定可分配，后写的覆盖先写的。乐观锁 `version` 能检测冲突但会抛 OptimisticLockException，用户体验差。

### 修复

**1. 所有状态变更操作必须通过专用的带锁查询方法**

```java
// PhoneRepository.java — 每个操作一个专用查询
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PhoneNumber p WHERE p.id = :id")
Optional<PhoneNumber> findByIdForUpdate(@Param("id") Long id);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PhoneNumber p WHERE p.id = :id1 OR p.id = :id2")
List<PhoneNumber> findByIdsForUpdate(@Param("id1") Long id1, @Param("id2") Long id2);
```

**2. Service 层强制通过带锁方法获取实体**

```java
// PhoneService.java — 每个操作方法的模式
@Transactional
public PhoneNumber allocate(Long phoneId, AllocateRequest req, String operator) {
    // ❌ 禁止：phoneRepository.findById(phoneId)
    // ✅ 必须：
    PhoneNumber phone = phoneRepository.findByIdForUpdate(phoneId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
    // ... 业务逻辑
    phoneRepository.save(phone); // version 乐观锁作为第二道防线
}
```

**3. 新增需求 & 任务**

| 新增 | 内容 |
|------|------|
| **PH-35** | Repository 层所有变更操作使用 `@Lock(PESSIMISTIC_WRITE)` + `@Query` |
| **PH-36** | Service 层禁止直接 `findById()` 后做状态变更；Code Review 强制检查 |
| **T-020d** | 编写 PhoneRepository 专用锁查询方法（findByIdForUpdate / findByIdsForUpdate） |

### 验证

JMeter 2 线程并发 `POST /api/phones/1/allocate`：后到达的线程必须看到已被分配的号码状态已变 → 返回 PHONE_NOT_IDLE。`SELECT * FROM phone_history WHERE phone_id=1` 只有 1 条 allocate 记录。

---

## R2：change-number 事务断裂 — 两个号码不在同一事务

### 根因

change-number 涉及两条 UPDATE：旧号码→idle + 新号码→active。如果 `@Transactional` 未包裹整个方法，或内部调用了非事务方法，或数据库中途宕机，可能造成：
- 旧号码已 idle、新号码未 active → 员工失去号码
- 旧号码未 idle、新号码已 active → 两个号码都属于同一员工

### 修复

**1. 单一事务方法，禁止拆分**

```java
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public ChangeNumberResult changeNumber(Long currentPhoneId, Long newPhoneId, 
                                        ChangeNumberRequest req, String operator) {
    // Step 1: 同时锁两个号码（一条 SQL，减少死锁概率）
    List<PhoneNumber> phones = phoneRepository.findByIdsForUpdate(currentPhoneId, newPhoneId);
    PhoneNumber current = phones.stream().filter(p -> p.getId().equals(currentPhoneId)).findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
    PhoneNumber newPhone = phones.stream().filter(p -> p.getId().equals(newPhoneId)).findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_FOUND));
    
    // Step 2: 状态校验（在事务内）
    if (current.getStatus() != PhoneStatus.ACTIVE) throw ...;
    if (newPhone.getStatus() != PhoneStatus.IDLE) throw ...;
    
    // Step 3: 原子操作（全部在一个 try 块内）
    // 旧号码 → idle
    current.setStatus(PhoneStatus.IDLE);
    current.setUserId(null);
    current.setExtensionNumber(null);
    current.setExtensionType(null);
    phoneHistoryRepository.save(buildHistory(current, "change_number", ...));
    
    // 新号码 → active
    newPhone.setStatus(PhoneStatus.ACTIVE);
    newPhone.setUserId(currentUserId);
    // 分机号按员工重新判定
    applyExtension(newPhone, employee);
    phoneHistoryRepository.save(buildHistory(newPhone, "change_number", ...));
    
    phoneRepository.saveAll(List.of(current, newPhone));
    // Spring 在方法结束时 COMMIT；任何异常触发 ROLLBACK
}
```

**2. 死锁预防：固定锁定顺序**

两个号码的 `findByIdsForUpdate` 使用 `IN (:id1, :id2)` + `ORDER BY p.id ASC`，确保总是先锁小 ID 再锁大 ID，避免 A→B 和 B→A 交叉死锁。

**3. 新增需求 & 任务**

| 新增 | 内容 |
|------|------|
| **PH-37** | change-number 必须单一 @Transactional；双号码一次查询锁定；固定 ORDER BY id 顺序 |
| **PH-38** | Service 层 changeNumber 方法无内部非事务调用；全部逻辑在一个方法体内 |
| **T-024d** | changeNumber 重构为单事务方法 + 死锁预防 |

### 验证

1. 正常流程：change-number(1→2) → 号码1 idle、号码2 active、员工持有号码2
2. 中间抛异常：在 service 方法内 `throw new RuntimeException("模拟故障")` → 号码1 仍 active、号码2 仍 idle、事务完全回滚
3. 死锁测试：两个线程同时 change-number(1→2) 和 change-number(2→1) → 一个成功一个失败，无不一致

---

## R3：分机号池耗尽无预警 — Admin 分配时才知道

### 根因

当前设计：Admin 分配号码 → 号池随机选 → 失败 → 返回 EXT_POOL_EXHAUSTED。Admin 只能被动发现，无法提前预防。

### 修复

**1. 号池列表增加三色预警**

| 用量 | 颜色 | 含义 | 动作 |
|------|------|------|------|
| < 60% | 🟢 绿色 | 充裕 | 无 |
| 60-80% | 🟡 黄色 | 紧张 | 显示"建议扩充"提示 |
| > 80% | 🔴 红色 | 即将耗尽 | 推送通知给部门 Admin；高亮显示 |
| = 100% | ⬛ 黑色 | 已耗尽 | 禁止分配；推送通知 |

**2. 主动通知机制**

号池用量跨过 80% 阈值时，系统自动推送 `NOT-04` 通知给：
- 该部门 Admin
- 所有 Ops

**3. 分配时的友好提示**

```json
// 当前：无号池 → EXT_POOL_REQUIRED
// 修复后：
{
  "code": 400,
  "errorCode": "EXT_POOL_EXHAUSTED",
  "message": "该部门号池已耗尽（已用100/总数100）。请联系运维人员扩充号池。",
  "data": {
    "orgId": 5,
    "orgName": "技术部",
    "poolUsed": 100,
    "poolTotal": 100,
    "suggestion": "建议扩充范围至 101000-102000（+1000个）"
  }
}
```

**4. 新增需求 & 任务**

| 新增 | 内容 | P |
|------|------|:--:|
| **EP-09** | 号池用量三色预警（绿<60%/黄60-80%/红>80%/黑=100%） | P0 |
| **EP-10** | 跨 80% 阈值时自动推送通知给部门 Admin + Ops | P1 |
| **EP-11** | EXT_POOL_EXHAUSTED 返回建议扩充范围 | P1 |
| **NOT-04** | 号池预警通知 | P1 |
| **T-029c** | 号池用量计算 + 三色标签组件 |
| **T-029d** | 阈值跨越检测 + 自动推送通知 |

### 验证

1. 号池 100000-100009（10个）→ 分配 8 个后 → 列表显示 🔴 80%（8/10）
2. 分配第 9 个 → 跨 80% → Admin + Ops 收到通知
3. 分配第 10 个 → ⬛ 100% → 再分配 → EXT_POOL_EXHAUSTED + 建议范围

---

## R4：组织 path 超 VARCHAR(500) — 插入失败

### 根因

`org_structure.path` VARCHAR(500)。组织 ID 为 BIGINT（最长 19 位），path 格式 `/1/22/333/...`，极端情况下 500/(19+1) ≈ 25 层就会溢出。当前无前置校验，INSERT 直接抛 SQL 异常。

### 修复

**1. 创建前预校验最大深度**

```java
// OrgService.create()
public OrgStructure create(OrgRequest req) {
    if (req.getParentId() != null) {
        OrgStructure parent = orgRepository.findById(req.getParentId())
            .orElseThrow(...);
        
        // 预计算新 path
        // 假设 parent.level + 1 后 ID 最长为 19 位
        // path = parent.path + "<future_id>" + "/"
        // 保守预留 ID 长度 = 19，即 path 格式最大 +20 个字符
        if (parent.getPath().length() + 20 > 500) {
            throw new BusinessException(ErrorCode.ORG_DEPTH_EXCEEDED, 
                "组织层级过深（当前 " + parent.getLevel() + " 层，最多支持约 25 层）");
        }
    }
    // ... 正常创建
}
```

**2. DDL 保护：path 字段增加长度（防御性）**

```sql
-- 如果尚未建表，将 VARCHAR(500) → VARCHAR(1000)
-- 500 是保守值但够用；如果已建表则保持
ALTER TABLE org_structure MODIFY COLUMN path VARCHAR(1000);
```

**3. 新增需求 & 任务**

| 新增 | 内容 | P |
|------|------|:--:|
| **ORG-08** | 创建组织前预校验 path 长度（最多 ~25层）；超过返回 ORG_DEPTH_EXCEEDED | P0 |
| **DB-01** | org_structure.path VARCHAR(500) → 1000（防御性冗余） | P1 |
| **T-012d** | OrgService.create 增加 depth 预校验 |

### 验证

1. 创建 25 层组织（每层 ID < 1000）→ 全部成功
2. 创建第 26 层 → 返回 ORG_DEPTH_EXCEEDED "组织层级过深（当前25层，最多支持约25层）"
3. path 字段长度检查：`SELECT MAX(LENGTH(path)) FROM org_structure` < 1000

---

## R5：Excel 导入 500 行同步超时 — HTTP 超时或 OOM

### 根因

500 行 × 每行多次 DB 查询（判重 + 组织匹配 + surrender 检测）→ 串行同步 → 可能 >30s → HTTP 超时。且所有数据在内存中，大文件可能 OOM。

### 修复

**1. 改为异步处理 + 轮询模式**

```
[Ops 上传 Excel] → [后端立即返回 batch_id + "处理中"] → [异步线程逐行处理]
→ [Ops 轮询 GET /api/import/{batchId}/status] → [返回进度 + 预览数据]
→ [Ops 确认] → [POST /api/import/{batchId}/confirm] → [批量 INSERT]
```

```java
// 新增：导入批次表
CREATE TABLE import_batch (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    batch_id    VARCHAR(50) NOT NULL UNIQUE,
    file_name   VARCHAR(200) NOT NULL,
    status      ENUM('processing','ready','confirmed','failed') NOT NULL DEFAULT 'processing',
    total_rows  INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    pending_rows INT NOT NULL DEFAULT 0,
    error_rows  INT NOT NULL DEFAULT 0,
    uploaded_by VARCHAR(50) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**2. 批量查询优化（N+1 → 2 次查询）**

```java
// ❌ 逐行查：500 × SELECT FROM org_structure WHERE name = ?
// ✅ 批量查：1 次加载所有 org → HashMap<String, OrgStructure>
List<OrgStructure> allOrgs = orgRepository.findAll();
Map<String, OrgStructure> orgMap = allOrgs.stream()
    .collect(Collectors.toMap(OrgStructure::getName, Function.identity()));

// ❌ 逐行查：500 × SELECT FROM phone_number WHERE phone_number = ?
// ✅ 批量查：1 次 IN 查询
List<String> phoneNumbers = rows.stream().map(Row::getPhoneNumber).toList();
List<PhoneNumber> existing = phoneRepository.findByPhoneNumberIn(phoneNumbers);
Set<String> existingSet = existing.stream().map(PhoneNumber::getPhoneNumber).collect(Collectors.toSet());
```

**3. 确认后批量 INSERT**

```java
// 不是逐条 save，而是 JdbcTemplate batchInsert
jdbcTemplate.batchUpdate(
    "INSERT INTO phone_number (phone_number, org_id, status, ...) VALUES (?, ?, 'idle', ...)",
    rowsToInsert,
    100  // batch size
);
```

**4. 新增需求 & 任务**

| 新增 | 内容 | P |
|------|------|:--:|
| **PH-06** | 导入改为异步：上传→batch_id→轮询进度→预览→确认→批量INSERT | P0 |
| **PH-07** | 导入批次表 import_batch | P0 |
| **PH-08** | 批量查询优化（org 一次加载、phone 一次 IN 查询） | P0 |
| **PH-09** | 确认后 JdbcTemplate batchInsert（batchSize=100） | P0 |
| **T-030a** | 创建 import_batch 表 + entity |
| **T-030b** | ImportService 异步处理 + 进度查询 |
| **T-030c** | 批量查询优化（HashMap 缓存 + IN 查询） |
| **T-030d** | 前端导入页：上传→进度条→预览→确认 |
| **T-030e** | 导入进度轮询 API |

### 验证

1. 上传 500 行 → `/api/import/upload` 立即返回 batch_id（< 200ms）
2. 轮询 `GET /api/import/{batchId}/status` → processing → ready（< 5s）
3. 预览返回 500 行：480 success + 15 pending + 5 error
4. 确认提交 → 480 行批量 INSERT → phone_number 表新增 480 条
5. import_batch 记录完整

---

## 六、修复变更汇总

### 新增需求 ID（10 个）

| ID | 描述 | P |
|----|------|:--:|
| PH-35 | Repository 加 @Lock PESSIMISTIC_WRITE | P0 |
| PH-36 | Service 禁止非锁 findById + 状态变更 | P0 |
| PH-37 | change-number 单事务 + ORDER BY id 防死锁 | P0 |
| PH-38 | changeNumber 全逻辑一个方法体 | P0 |
| EP-09 | 号池三色预警 | P0 |
| EP-10 | 80%阈值自动推送通知 | P1 |
| EP-11 | 耗尽返回建议扩充范围 | P1 |
| PH-06 | 导入改为异步+轮询 | P0 |
| PH-07 | 导入批次表 import_batch | P0 |
| PH-08 | 批量查询优化 | P0 |
| PH-09 | 批量 INSERT（batchSize=100） | P0 |
| ORG-08 | 创建前预校验 path 深度 | P0 |
| DB-01 | path VARCHAR(500)→1000 | P1 |
| NOT-04 | 号池预警通知 | P1 |

### 新增任务（12 个）

| 任务 | 描述 |
|------|------|
| T-020d | PhoneRepository 专用锁查询方法 |
| T-024d | changeNumber 单事务重构 + 死锁预防 |
| T-029c | 号池用量计算 + 三色标签 |
| T-029d | 阈值跨越检测 + 通知 |
| T-030a | import_batch 表 + entity |
| T-030b | ImportService 异步处理 |
| T-030c | 批量查询优化 |
| T-030d | 前端导入进度页 |
| T-030e | 导入进度轮询 API |
| T-012d | OrgService depth 预校验 |
| T-005c | org_structure.path 扩 VARCHAR(1000) |
| T-025c | 号池预警通知 NOT-04 |

---

## 七、修复后的并发策略（更新）

| 场景 | 策略 | 死锁预防 |
|------|------|---------|
| allocate | `findByIdForUpdate` 锁号码 + 员工 | — |
| reclaim / trouble / surrender / 预留 / 禁用 | `findByIdForUpdate` 锁号码 | — |
| change-user | `findByIdForUpdate` 锁号码 + 新旧员工 | — |
| **change-number** | `findByIdsForUpdate` 双号码**一次查询**锁定 | `ORDER BY p.id ASC` 固定锁顺序 |
| 所有变更 | @Transactional(READ_COMMITTED) + version 二道防线 | — |

---

## 八、修复后的风险等级

| # | 风险 | 修复前 | 修复后 | 关键措施 |
|---|------|:--:|:--:|------|
| R1 | 并发分配冲突 | 🔴 | 🟢 | @Lock PESSIMISTIC_WRITE + Code Review 强制 |
| R2 | change-number 事务断裂 | 🔴 | 🟢 | 单事务 + 双号码一次锁 + ORDER BY 防死锁 |
| R3 | 号池耗尽无预警 | 🔴 | 🟡 | 三色预警 + 阈值通知 + 友好提示 |
| R4 | path 超长 | 🔴 | 🟢 | 创建前预校验 + VARCHAR 1000 防御 |
| R5 | 导入超时 | 🔴 | 🟡 | 异步+轮询 + 批量查询 + batchInsert |

> R3/R5 降为 🟡 是因为异步和预警机制已到位，但需要运维关注响应，不是完全自动化。
