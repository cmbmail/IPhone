#!/bin/bash

echo "======================================"
echo "    PhoneBiz 一键部署脚本"
echo "    招商银行电话业务系统"
echo "======================================"
echo ""

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_HOME="$APP_HOME"
FRONTEND_HOME="$APP_HOME/frontend"
SERVER_IP="192.168.139.161"

echo "项目目录: $APP_HOME"
echo "服务器IP: $SERVER_IP"
echo ""

check_nodejs() {
    if ! command -v node &> /dev/null; then
        return 1
    fi
    local version=$(node --version | sed 's/v//')
    local major=$(echo $version | cut -d. -f1)
    if [ "$major" -lt 18 ]; then
        return 2
    fi
    return 0
}

check_java() {
    if ! command -v java &> /dev/null; then
        return 1
    fi
    local version=$(java -version 2>&1 | head -1 | grep -oP '(?<=")1\.8')
    if [ -z "$version" ]; then
        return 1
    fi
    return 0
}

check_maven() {
    if ! command -v mvn &> /dev/null; then
        return 1
    fi
    return 0
}

check_nginx() {
    if ! command -v nginx &> /dev/null; then
        return 1
    fi
    return 0
}

echo "=== 环境检查 ==="

if check_java; then
    echo "✓ Java 已安装: $(java -version 2>&1 | head -1)"
else
    echo "✗ Java 未安装或版本不正确，需要 Java 8"
    exit 1
fi

if check_maven; then
    echo "✓ Maven 已安装: $(mvn -version | head -1)"
else
    echo "✗ Maven 未安装"
    exit 1
fi

if check_nginx; then
    echo "✓ Nginx 已安装: $(nginx -v 2>&1)"
else
    echo "✗ Nginx 未安装"
    echo "安装命令: sudo yum install -y nginx"
    exit 1
fi

echo ""

echo "=== 后端构建 ==="
cd "$BACKEND_HOME"
echo "编译后端项目..."
mvn clean package -DskipTests

if [ ! -f "target/phonebiz-1.0.0-SNAPSHOT.jar" ]; then
    echo "✗ 后端构建失败"
    exit 1
fi
echo "✓ 后端构建成功"

echo ""

if check_nodejs; then
    echo "✓ Node.js 已安装: $(node --version)"
    echo "=== 前端构建 ==="
    cd "$FRONTEND_HOME"
    
    if [ ! -d "node_modules" ]; then
        echo "安装前端依赖..."
        npm install
    fi
    
    echo "构建前端项目..."
    npm run build
    
    if [ ! -d "dist" ]; then
        echo "✗ 前端构建失败"
        exit 1
    fi
    echo "✓ 前端构建成功"
else
    echo "⚠ Node.js 版本过低或未安装，跳过前端构建"
    echo "请手动升级 Node.js 到 v18+ 后执行: cd $FRONTEND_HOME && npm install && npm run build"
fi

echo ""

echo "=== 配置 Nginx ==="
NGINX_CONF="/etc/nginx/conf.d/phonebiz.conf"

if [ -f "$NGINX_CONF" ]; then
    echo "备份现有配置..."
    cp "$NGINX_CONF" "${NGINX_CONF}.bak.$(date +%Y%m%d%H%M%S)"
fi

echo "复制 Nginx 配置..."
cp "$APP_HOME/nginx/phonebiz.conf" "$NGINX_CONF"

echo "测试 Nginx 配置..."
nginx -t

if [ $? -ne 0 ]; then
    echo "✗ Nginx 配置测试失败"
    exit 1
fi
echo "✓ Nginx 配置测试通过"

echo ""

echo "=== 启动服务 ==="

echo "重启后端服务..."
sh "$APP_HOME/shell/start.sh" restart

echo "重启 Nginx..."
if systemctl is-active --quiet nginx; then
    systemctl reload nginx
else
    systemctl start nginx
fi

echo ""

echo "======================================"
echo "    部署完成!"
echo "======================================"
echo ""
echo "访问地址:"
echo "  主入口: http://$SERVER_IP"
echo "  后端API: http://$SERVER_IP/phonebiz"
echo ""
echo "服务状态:"
sh "$APP_HOME/shell/start.sh" status
echo ""
echo "登录账号: admin / admin123"
echo "======================================"
