# PhoneBiz 企业内线电话业务管理平台 - 部署指南

## 环境要求

| 依赖 | 版本 |
|------|------|
| Java | 21+ |
| MySQL | 8.0+ |
| Nginx | 1.20+ |

## 部署步骤

### 1. 连接服务器

```bash
ssh root@192.168.139.105
密码: 1qaz@WSX
```

### 2. 安装依赖

```bash
# 安装 Java 21
yum install -y java-21-openjdk

# 安装 MySQL 8.0
yum install -y mysql-community-server

# 安装 Nginx
yum install -y nginx
```

### 3. 配置 MySQL

```bash
# 启动 MySQL
systemctl start mysqld
systemctl enable mysqld

# 设置 root 密码
mysql -u root -p
# 输入临时密码（可通过 grep 'temporary password' /var/log/mysqld.log 查询）

# 创建数据库
CREATE DATABASE phonebiz CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（可选）
CREATE USER 'phonebiz'@'localhost' IDENTIFIED BY '1qaz@WSX';
GRANT ALL PRIVILEGES ON phonebiz.* TO 'phonebiz'@'localhost';
FLUSH PRIVILEGES;
```

### 4. 上传部署文件

将以下文件上传到服务器 /root 目录：
- `phonebiz-1.0.0.jar` (后端JAR包)
- `phonebiz-frontend.tar.gz` (前端压缩包)
- `deploy.sh` (部署脚本)

```bash
# 方式1: 使用 scp 上传（在本地执行）
scp -r backend/build/libs/phonebiz-1.0.0.jar root@192.168.139.105:/root/
scp -r frontend/dist.tar.gz root@192.168.139.105:/root/phonebiz-frontend.tar.gz
scp deploy.sh root@192.168.139.105:/root/
```

### 5. 执行部署脚本

```bash
chmod +x deploy.sh
./deploy.sh
```

### 6. 验证部署

```bash
# 检查后端服务状态
systemctl status phonebiz

# 检查 Nginx 状态
systemctl status nginx

# 查看日志
tail -f /var/log/messages | grep phonebiz
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://192.168.139.105 |
| API文档 | http://192.168.139.105/swagger-ui.html |

## 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| ops | ops123 | 操作员 |
| boss | boss123 | 老板 |

## 服务管理

```bash
# 启动服务
systemctl start phonebiz

# 停止服务
systemctl stop phonebiz

# 重启服务
systemctl restart phonebiz

# 查看日志
journalctl -u phonebiz -f
```

## 项目结构

```
/opt/phonebiz/
├── backend/
│   └── phonebiz-1.0.0.jar
└── frontend/
    ├── index.html
    └── assets/
        ├── index-*.css
        └── index-*.js
```

## 配置说明

### 后端配置文件

配置文件路径: `/opt/phonebiz/backend/application.yml`

主要配置项:
- `server.port`: 服务端口（默认8080）
- `spring.datasource.*`: 数据库连接配置
- `jwt.secret`: JWT密钥
- `jwt.expiration`: Token有效期（毫秒）

### Nginx配置

配置文件路径: `/etc/nginx/conf.d/phonebiz.conf`

包含前端静态文件服务和API反向代理配置。

## 故障排查

### 数据库连接失败
1. 检查MySQL服务是否启动
2. 检查数据库密码是否正确
3. 检查防火墙是否开放3306端口

### 服务启动失败
1. 检查Java版本是否正确
2. 检查端口8080是否被占用
3. 查看日志: `journalctl -u phonebiz`

### 前端页面无法访问
1. 检查Nginx服务状态
2. 检查前端文件是否正确解压
3. 检查防火墙是否开放80端口