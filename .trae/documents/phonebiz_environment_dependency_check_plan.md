# PhoneBiz 软件版本依赖与环境检查计划

## 1. 版本锁定对比与依赖检查

### 1.1 文档中定义的依赖版本（唯一真源）

| 环节 | 组件 | 文档定义版本 | 锁定级别 | 文件 |
|------|------|-------------|---------|------|
| Java 运行时 | OpenJDK | 17 LTS | 🔴 精确 | `java -version` |
| 后端框架 | Spring Boot | 3.2.5 | 🔴 精确 | `build.gradle` |
| 构建工具 | Gradle | 8.7 | 🔴 精确 | `gradle --version` |
| 数据库 | MySQL | 8.0.36 | 🟡 次版本 | `SELECT VERSION()` |
| DB 迁移 | Flyway | Spring 托管 | 🟢 托管 | 父 POM 管理 |
| Spring Java Format | 0.0.43 | 🔴 精确 | `build.gradle` |
| CheckStyle | 10.17.0 | 🔴 精确 | `build.gradle` |
| OWASP Dependency Check | 9.2.0 | 🔴 精确 | `build.gradle` |
| Docker Compose | 2.24 | 🟡 次版本 | `docker-compose --version` |
| **前端** | | | | |
| Node.js | 20 LTS | 🔴 LTS | `node -v` |
| React | 18.3.1 | 🔴 次版本 | `package.json` |
| Ant Design | 5.22.3 | 🔴 次版本 | `package.json` |
| Vite | 5.4.11 | 🔴 次版本 | `package.json` |
| TypeScript | 5.6.3 | 🔴 次版本 | `package.json` |
| @tanstack/react-query | 5.62.0 | 🟡 次版本 | `package.json` |
| Zustand | 5.0.1 | 🟡 次版本 | `package.json` |
| axios | 1.7.9 | 🟡 次版本 | `package.json` |
| Prettier | 3.4.2 | 🔴 精确 | `package.json` |
| ESLint | 9.15.0 | 🔴 精确 | `package.json` |

### 1.2 潜在版本冲突检查

| 组件 | 潜在冲突点 | 风险 | 处理方案 |
|------|-----------|------|---------|
| **Spring Boot 3.2.5 vs Java 17** | Spring Boot 3.x 要求 Java 17+ | ✅ 无冲突 | 版本兼容 |
| **Spring Boot 3.2.5 与 Flyway** | Spring Boot 3 与 Flyway 9.x/10.x 兼容 | ✅ Spring 托管 | 父 POM 自动管理 |
| **React 18.3.1 vs Ant Design 5.22.3** | Antd 5 完全支持 React 18 | ✅ 无冲突 | 已知兼容组合 |
| **Vite 5.4.11 vs React 18** | Vite 5 完美支持 React 18 | ✅ 无冲突 | 已广泛测试 |
| **TypeScript 5.6.3 vs React 18** | TS 5.x 与 React 18 类型完全兼容 | ✅ 无冲突 | |

### 1.3 禁止升级的依赖

| 组件 | 原因 | 禁止升级到 |
|------|------|-----------|
| React | React 19 移除 forwardRef/旧 Context，Antd 5 部分组件可能不兼容；所有代码示例基于 18 | ❌ React 19 |

---

## 2. 当前项目与规范的差异（如果有现有代码）

| 维度 | 文档规范要求 | 现有项目（如果有） | 差距分析 |
|------|-------------|------------------|---------|
| 后端技术栈 | Spring Boot 3.2.5 + JPA + Gradle + Java 17 | Spring Boot 2.7.18 + MyBatis Plus + Maven + Java 8 | 🔴 完全不匹配，需重构 |
| 状态机 | 6 种状态（idle/active/stopped/cancelled/reserved/disabled） | 自定义 7 种 | 🔴 需统一 |
| 权限体系 | Spring Security + @PreAuthorize | 自定义拦截器 + JWT | ⚠️ 需评估重构 |
| 数据库结构 | 24 张表完整设计（含乐观锁等） | 8 张表简化结构 | 🔴 需补充 |

---

## 3. 环境检查清单（Phase 0 - 环境准备）

### 3.1 系统要求

| 环境 | 要求 | 检查项 |
|------|------|-------|
| **操作系统** | 主流 Linux/Mac/Windows | 不要求 |
| **磁盘空间** | 后端 500MB+，前端 2GB+ | 剩余空间 ≥10GB |
| **内存** | 4GB+ | 8GB+ 推荐 |

### 3.2 后端环境检查

| 检查项 | 验证命令 | 预期结果 | 状态 |
|--------|---------|---------|------|
| Java 17 安装 | `java -version` | OpenJDK 17.x.x | 🔄 待检查 |
| JAVA_HOME 配置 | `echo $JAVA_HOME` | 指向 JDK 17 | 🔄 待检查 |
| Gradle 8.7 安装 | `gradle --version` | 8.7 | 🔄 待检查 |
| MySQL 8 安装 | `mysql --version` | 8.0.x | 🔄 待检查 |
| Git 安装 | `git --version` | 任意（推荐 ≥2.x） | 🔄 待检查 |
| Docker（可选） | `docker --version` | 20.10+ | 🔄 待检查 |
| Docker Compose（可选） | `docker-compose --version` | 2.24+ | 🔄 待检查 |

### 3.3 前端环境检查

| 检查项 | 验证命令 | 预期结果 | 状态 |
|--------|---------|---------|------|
| Node.js 20 LTS | `node -v` | v20.x.x | 🔄 待检查 |
| npm 安装 | `npm -v` | 9.x+ | 🔄 待检查 |

---

## 4. 执行计划

### Phase 0 - 环境准备

1. **检查当前环境**
   - 逐一验证 3.2 和 3.3 中的检查项
   - 记录环境版本信息

2. **版本安装/升级**
   - 安装缺失的环境组件
   - 升级不符合要求的组件

3. **确认环境**
   - 再次运行所有检查项
   - 确保全部通过

### 预期产出

- `environment_checklist.md`：环境检查报告
- 符合所有版本要求的开发环境

---

## 5. 风险与注意事项

| 风险 | 影响 | 缓解方案 |
|------|------|---------|
| 现有项目与规范技术栈完全不匹配 | 重构工作量大 | ⚠️ 建议完全按 v3 规范重写，而非修补现有代码 |
| 版本兼容性问题 | 开发受阻 | 严格使用文档中的版本号，禁止随意升级 |
| 环境配置耗时 | 影响开发进度 | 提供 Docker Compose 配置，快速搭建开发环境 |
