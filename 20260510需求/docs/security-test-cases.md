# PhoneBiz 安全测试用例清单

> 本文档定义 PhoneBiz Phase 1/2/3 交付前的安全测试门禁用例。
> 测试工具建议：SQL 注入/XSS → Burp Suite Professional / OWASP ZAP；CSRF → CSRF Tester。
> 所有用例必须通过才可进入 Phase/发布门禁。

---

## 1. SQL 注入测试

### 1.1 测试范围

所有接受用户输入的 API 端点（GET/POST/PUT/DELETE），重点覆盖：

| 模块 | 端点类型 | 测试字段 |
|------|---------|---------|
| M02 组织架构 | GET /api/orgs?q= | 组织名称搜索 |
| M03 员工管理 | GET /api/employees?q= | 员工姓名、工号搜索 |
| M06 号码基础 | GET /api/phone-numbers?q= | 号码搜索 |
| M12 号码导入 | POST /api/imports | Excel 文件上传 |
| M18 工单 | POST /api/work-orders | 工单创建 |

### 1.2 测试用例

| ID | 输入 | 预期行为 |
|----|------|---------|
| SQL-001 | `q=' OR 1=1 --` | 返回 401/403（鉴权拒绝）或空结果，绝非 200 + 全量数据 |
| SQL-002 | `q=' OR 'a'='a` | 同上 |
| SQL-003 | `q=123'; DROP TABLE phone_number; --` | HTTP 500 + 应用层错误日志，绝无 DB 层报错泄漏 |
| SQL-004 | `q=1' AND 1=1 --` | 正常处理（参数化查询生效） |
| SQL-005 | Content-Type JSON：`{"phoneNumber":"'; DELETE FROM employee; --"}` | 同 SQL-003 |
| SQL-006 | 文件上传（Excel）：单元格含 `=HYPERLINK("http://evil.com","x")` | Excel 公式注入 → 导入时校验公式语法，拒绝含 `=` 开头的数据行 |

### 1.3 通过标准

- [ ] 所有输入点均使用 JPA 参数化查询（无拼接 SQL）
- [ ] 错误响应不暴露 SQL 堆栈
- [ ] 响应码统一（400 参数校验 / 401 鉴权 / 403 权限）
- [ ] SQL-006 Excel 公式注入防护已实现

---

## 2. XSS 跨站脚本测试

### 2.1 测试范围

所有文本输入字段：

| 字段 | 位置 |
|------|------|
| 组织名称 | M02 创建/编辑 |
| 员工姓名/工号备注 | M03 |
| 号码备注 | M06/M09/M10 |
| 工单描述 | M18 |
| 发票备注 | M25/M26 |

### 2.2 测试用例

| ID | 输入 | 预期行为 |
|----|------|---------|
| XSS-001 | `<script>alert(1)</script>` | 存储型 XSS：列表页/详情页显示时，文本被转义，不弹窗 |
| XSS-002 | `<img src=x onerror=alert(1)>` | 同上，被转义 |
| XSS-003 | `javascript:alert(1)` | 链接字段（如有），不执行 |
| XSS-004 | `<svg onload=alert(1)>` | 被转义 |
| XSS-005 | 前后端双重编码：`%3Cscript%3Ealert(1)%3C/script%3E` | 后端正确解码后再转义，不执行 |

### 2.3 通过标准

- [ ] 所有文本输入入库前转义或前端渲染时转义
- [ ] 富文本编辑器（如有）使用白名单标签过滤
- [ ] 日志不执行注入脚本

---

## 3. CSRF 跨站请求伪造测试

### 3.1 测试范围

所有状态变更端点（POST/PUT/DELETE）：

| 模块 | 端点 |
|------|------|
| M04 认证 | POST /api/auth/login（登录劫持） |
| M09 号码分配 | POST /api/phones/{id}/allocate |
| M10 状态变更 | POST /api/phones/{id}/surrender |
| M12 号码导入 | POST /api/imports/upload |
| M18 工单 | POST /api/work-orders |

### 3.2 测试用例

| ID | 场景 | 预期行为 |
|----|------|---------|
| CSRF-001 | 在恶意页面发起 POST /api/phones/1/allocate | 后端验证 CSRF Token，拒绝无 Token 请求（403） |
| CSRF-002 | 无 Cookie 的 API 调用（JWT Bearer Token） | 若无 CSRF Token Header，验证是否检查 SameSite Cookie |
| CSRF-003 | 文件上传 CSRF | 同上，403 拒绝 |

### 3.3 防护实现要求

- [ ] Spring Security 配置 `CsrfTokenRepository`（或 JWT 场景下自定义 CSRF 检查）
- [ ] 前端每次请求在 Header 携带 CSRF Token
- [ ] Cookie 设置 `SameSite=Lax` 或 `Strict`
- [ ] 文档明确标注哪些端点豁免 CSRF（如只读 GET）

### 3.4 通过标准

- [ ] 所有 POST/PUT/DELETE 端点有 CSRF 防护
- [ ] 使用 Burp Suite CSRF Token Tester 验证

---

## 4. 认证与会话安全测试

| ID | 测试项 | 预期行为 |
|----|--------|---------|
| AUTH-001 | JWT 超时测试 | 过期 Token 返回 401，自动跳转登录 |
| AUTH-002 | JWT 重放测试 | 使用同一 Token 两次请求，第二次应拒绝（`jti` 唯一 ID） |
| AUTH-003 | 暴力破解 | 5 次错误密码后账号锁定 30 分钟 |
| AUTH-004 | 权限绕过 | 无权限用户访问 Admin 端点 → 403 |
| AUTH-005 | Scope 越权 | A 部门 Admin 访问 B 部门数据 → 403 或空结果 |

---

## 5. 文件上传安全测试

| ID | 文件类型 | 预期行为 |
|----|---------|---------|
| FILE-001 | `.exe` 文件上传 | 后端校验 MIME，文件被拒绝，错误码返回 |
| FILE-002 | `.jsp` 文件伪装 `.xlsx` | 后端二次校验（魔数/文件头），拒绝 |
| FILE-003 | 超大文件（100MB） | 超过 10MB 限制，413 或前端拦截 |
| FILE-004 | 文件名含 `../`（路径穿越） | 后端过滤 `..` 路径遍历字符，文件存储安全 |

---

## 6. 测试执行记录

| 日期 | 测试人 | Phase | 通过率 | 遗留问题 |
|------|--------|-------|--------|---------|
| — | — | Phase 1 | — | — |
| — | — | Phase 2 | — | — |
| — | — | Phase 3 | — | — |

> 测试通过标准：全部用例通过（100%），任何 P0/P1 漏洞必须在发布前修复。
