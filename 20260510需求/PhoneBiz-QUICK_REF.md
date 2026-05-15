# PhoneBiz Phase 1 — 快速参考卡

> 一页纸速查，开发/审查时用。详细规则见需求清单 v5.1。

## 状态机

```
idle ←→ reserved(预留) / disabled(禁用)
idle → active ⇌ stopped → idle
any → cancelled（不可逆，可二次入库）
```

## API 端点（26 个）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/auth/login | 登录 | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/me | 当前用户 | 登录 |
| GET | /api/orgs/tree | 组织树 | 登录 |
| GET/POST/PUT/DELETE | /api/orgs/{id} | 组织CRUD | Admin scope |
| GET/POST/PUT/DELETE | /api/employees | 员工CRUD | Admin scope |
| GET | /api/phones | 号码列表 | Admin/Ops |
| GET | /api/phones/{id} | 号码详情 | Admin/Ops |
| POST | /api/phones | 新增号码 | Ops |
| POST | /api/phones/{id}/allocate | 分配 | Admin scope |
| POST | /api/phones/{id}/reclaim | 回收 | Admin scope |
| POST | /api/phones/{id}/trouble | 停复机 | Admin/Ops |
| POST | /api/phones/{id}/surrender | 拆机 | Admin/Ops |
| POST | /api/phones/{id}/change-user | 过户 | Admin scope |
| POST | /api/phones/{id}/change-number | 换号 | Admin scope |
| POST | /api/phones/{id}/change-org | 转移 | Admin scope |
| POST | /api/phones/{id}/reserve | 预留 | Admin scope |
| POST | .../release | 解除预留 | Admin scope |
| POST | /api/phones/{id}/disable | 禁用 | Admin scope |
| POST | .../enable | 解除禁用 | Admin scope |
| GET | /api/phones/{id}/history | 历史 | Admin/Ops |
| GET | /api/phones/surrendered | 已拆机 | Admin/Ops |
| GET/POST/PUT/DELETE | /api/ext-pools | 号池 | Ops |
| GET/POST/PUT/DELETE | /api/area-codes | 区号 | Ops |
| POST | /api/import/upload | 上传Excel | Ops |
| GET | /api/import/{batchId}/status | 导入进度 | Ops |
| POST | /api/import/{batchId}/confirm | 确认导入 | Ops |
| GET/POST/PUT | /api/users | 用户管理 | Admin |

## 并发策略（铁律）

| 操作 | 锁 | 死锁预防 |
|------|-----|---------|
| allocate | FOR UPDATE 号码+员工 | — |
| reclaim/trouble/surrender/预留/禁用 | FOR UPDATE 号码 | — |
| change-user | FOR UPDATE 号码+新旧员工 | — |
| **change-number** | FOR UPDATE **双号码一次查询** | ORDER BY id ASC |
| **禁止** `findById()` 后做状态变更 | 必须用专用 @Lock 方法 | — |

## 错误码（高频）

| 错误码 | 场景 |
|--------|------|
| EMP_ALREADY_HAS_PHONE | 分配/过户时员工已有号码 |
| PHONE_NOT_IDLE | 分配时号码非 idle |
| EXT_POOL_EXHAUSTED | 号池耗尽（含建议范围） |
| EXT_POOL_REQUIRED | 含字母工号但部门无号池 |
| OUT_OF_SCOPE | 操作不在 scope 内 |
| ACCOUNT_LOCKED/DISABLED | 账号锁定/禁用 |
| FIRST_LOGIN_CHANGE_PWD | 首次登录强制改密 |

## 分机号决策树

```
目标员工工号是纯数字？
├── 是 → auto（工号去前导0）→ 只校验当前使用中的 extension_number
└── 否 → 部门有号池？
    ├── 有 → manual（随机选6位，≤100次）→ 全局唯一含已拆机
    └── 无 → EXT_POOL_REQUIRED（提示配置号池）
```

## 按钮可见性

| 状态 | 按钮 |
|------|------|
| idle | 分配、预留、禁用、拆机 |
| reserved | 解除预留、拆机 |
| disabled | 解除禁用、拆机 |
| active | 回收、停机、过户、换号、转移、拆机 |
| stopped | 复机、回收、拆机 |
| cancelled | 仅查看+历史 |

## 响应格式

```json
{ "code": 200, "message": "success", "data": {...} }
{ "code": 400, "message": "错误描述", "errorCode": "ERROR_CODE" }
```

分页：`?page=1&size=20&sort=id,desc`

认证：`Authorization: Bearer <jwt_token>`
