# PhoneBiz 性能测试脚本

> 本目录存放 PhoneBiz Phase 1/2/3 性能压测脚本。
> 工具：JMeter（推荐）或 Gatling。脚本命名：`jmeter/{test-plan}.jmx` 或 `gatling/{simulation}.scala`

---

## 目录结构

```
docs/performance-tests/
├── README.md           ← 本文件
├── jmeter/             ← JMeter 脚本（.jmx）
│   ├── phonebiz-ph1-concurrency.jmx
│   └── phonebiz-ph1-load.jmx
└── gatling/            ← Gatling 脚本（可选）
    └── PhoneBizSimulation.scala
```

---

## 测试场景清单

### Phase 1 必须覆盖

| 场景 | 描述 | 指标 |
|------|------|------|
| **allocate-concurrent** | 100 并发同时分配同一号码 | 无重复分配 |
| **change-number-deadlock** | 10 并发同一号码对做换号 | 无 deadlock（超时 5s 内报错非卡死） |
| **pool-exhaustion** | 50 并发抢号至号池耗尽 | 公平分配，最后一个请求正确收到"号池耗尽" |
| **phone-list-p95** | 100 并发查询号码列表（1 万条数据） | P95 < 2s |
| **org-tree-p95** | 100 并发查询组织树（1000 节点） | P95 < 1s |
| **import-100** | 100 条号码导入 | < 5 分钟完成 |

### Phase 2 补充

| 场景 | 描述 |
|------|------|
| work-order-batch-create | 批量创建 100 个工单，验证拆分正确性 |
| snapshot-monthly | 月度快照定时任务执行时间 |

### Phase 3 补充

| 场景 | 描述 |
|------|------|
| invoice-ocr-throughput | 批量上传 50 张 PDF，OCR 处理吞吐量 |
| bill-allocation-accuracy | 账单分摊自动匹配率 |

---

## JMeter 使用说明

```bash
# 运行脚本
./jmeter/bin/jmeter -n -t docs/performance-tests/jmeter/phonebiz-ph1-concurrency.jmx -l results.jtl -e -o ./reports

# 查看报告
# 在 ./reports/index.html 中查看聚合报告
```

---

## 脚本维护

- 每次 Phase 门禁执行前，更新脚本中的 Token（从登录接口获取）
- 压测数据使用脱敏 Mock 数据（见 `src/main/resources/db/seed/`）
- **禁止在压测脚本中硬编码真实手机号、姓名**
