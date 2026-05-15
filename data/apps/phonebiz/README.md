# PhoneBiz - 招商银行电话业务系统

## 项目概述
本项目是招商银行电话业务系统的基础模块，包含组织架构、员工管理、权限管理和登录认证功能。

## 技术栈
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- MySQL 8.0
- JWT
- Lombok
- Hutool

## 项目结构
```
phonebiz/
├── src/main/java/com/cmbchina/phonebiz/
│   ├── annotation/          # 自定义注解
│   ├── aspect/              # AOP切面
│   ├── common/              # 公共类
│   ├── config/              # 配置类
│   ├── controller/          # 控制器
│   ├── dto/                 # 数据传输对象
│   ├── entity/              # 实体类
│   ├── interceptor/         # 拦截器
│   ├── mapper/              # 数据访问层
│   ├── service/             # 业务逻辑层
│   └── util/                # 工具类
├── src/main/resources/
│   ├── mapper/              # MyBatis映射文件
│   ├── sql/                 # SQL脚本
│   └── application.yml      # 配置文件
└── pom.xml
```

## 快速开始

### 1. 数据库初始化
执行 `src/main/resources/sql/init.sql` 初始化数据库：
```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

### 2. 修改数据库配置
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.139.161:3306/phonebiz
    username: root
    password: Cmb@123456
```

### 3. 启动项目
```bash
mvn clean package
java -jar target/phonebiz-1.0.0-SNAPSHOT.jar
```

或者使用Maven插件直接运行：
```bash
mvn spring-boot:run
```

## 默认账号
- 用户名：admin
- 密码：admin123

## API接口

### 登录认证
- `POST /phonebiz/auth/login` - 用户登录

### 组织架构
- `GET /phonebiz/org/list` - 查询组织列表
- `GET /phonebiz/org/{id}` - 查询组织详情
- `POST /phonebiz/org` - 新增组织
- `PUT /phonebiz/org` - 修改组织
- `DELETE /phonebiz/org/{id}` - 删除组织

### 员工管理
- `GET /phonebiz/employee/page` - 分页查询员工
- `GET /phonebiz/employee/{id}` - 查询员工详情
- `POST /phonebiz/employee` - 新增员工
- `PUT /phonebiz/employee` - 修改员工
- `DELETE /phonebiz/employee/{id}` - 删除员工

### 角色管理
- `GET /phonebiz/role/list` - 查询角色列表
- `GET /phonebiz/role/{id}` - 查询角色详情
- `POST /phonebiz/role` - 新增角色
- `PUT /phonebiz/role` - 修改角色
- `DELETE /phonebiz/role/{id}` - 删除角色

### 菜单管理
- `GET /phonebiz/menu/list` - 查询菜单列表
- `GET /phonebiz/menu/{id}` - 查询菜单详情
- `POST /phonebiz/menu` - 新增菜单
- `PUT /phonebiz/menu` - 修改菜单
- `DELETE /phonebiz/menu/{id}` - 删除菜单

## 开发规范
- 遵循招商银行Java开发规范
- 所有接口添加审计日志
- 使用JWT进行身份认证
- 使用逻辑删除
