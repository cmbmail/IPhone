#!/bin/bash

echo "======================================"
echo "    PhoneBiz Docker 部署脚本"
echo "    招商银行电话业务系统"
echo "======================================"
echo ""

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
DOCKER_HOME="$APP_HOME/docker"
SERVER_IP="192.168.139.161"
DOCKER_USER="phonebiz"

echo "项目目录: $APP_HOME"
echo "服务器IP: $SERVER_IP"
echo ""

check_docker() {
    if ! command -v docker &> /dev/null; then
        return 1
    fi
    return 0
}

check_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        return 0
    fi
    if docker compose version &> /dev/null; then
        return 0
    fi
    return 1
}

echo "=== 环境检查 ==="

if ! check_docker; then
    echo "✗ Docker 未安装"
    echo "安装命令:"
    echo "  sudo yum install -y docker"
    echo "  sudo systemctl start docker"
    echo "  sudo systemctl enable docker"
    exit 1
fi
echo "✓ Docker 已安装: $(docker --version)"

if ! check_docker_compose; then
    echo "✗ Docker Compose 未安装"
    echo "安装命令:"
    echo "  sudo yum install -y docker-compose"
    exit 1
fi
echo "✓ Docker Compose 已安装"

echo ""

echo "=== 安全检查: Docker 非 root 用户 ==="
if id "$DOCKER_USER" &>/dev/null; then
    echo "✓ 用户 $DOCKER_USER 已存在"
else
    echo "创建用户 $DOCKER_USER..."
    sudo useradd -r -s /bin/bash $DOCKER_USER
    sudo usermod -aG docker $DOCKER_USER
    echo "✓ 用户 $DOCKER_USER 创建完成"
fi

echo "添加当前用户到 docker 组..."
sudo usermod -aG docker $(whoami)

echo ""

echo "=== 后端构建 ==="
cd "$APP_HOME"
echo "编译后端项目..."
mvn clean package -DskipTests

if [ ! -f "target/phonebiz-1.0.0-SNAPSHOT.jar" ]; then
    echo "✗ 后端构建失败"
    exit 1
fi
echo "✓ 后端构建成功"

echo ""

echo "=== 启动 Docker 服务 ==="
cd "$DOCKER_HOME"

echo "停止现有容器..."
docker compose down 2>/dev/null || true

echo "构建并启动容器..."
docker compose up -d --build

echo ""
echo "=== 等待服务启动 ==="
sleep 30

echo ""
echo "=== 服务状态 ==="
docker compose ps

echo ""
echo "======================================"
echo "    Docker 部署完成!"
echo "======================================"
echo ""
echo "访问地址:"
echo "  主入口: http://$SERVER_IP"
echo "  后端API: http://$SERVER_IP/phonebiz"
echo ""
echo "Docker 命令:"
echo "  查看日志: cd $DOCKER_HOME && docker compose logs -f"
echo "  停止服务: cd $DOCKER_HOME && docker compose down"
echo "  重启服务: cd $DOCKER_HOME && docker compose restart"
echo ""
echo "注意: Redis 和 MySQL 容器使用非 root 用户运行"
echo "登录账号: admin / admin123"
echo "======================================"
