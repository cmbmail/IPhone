#!/bin/bash

echo "======================================"
echo "    前端部署脚本"
echo "======================================"
echo ""

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_HOME="$APP_HOME/frontend"

echo "项目目录: $FRONTEND_HOME"
echo ""

echo "=== 检查Node.js版本 ==="
NODE_VERSION=$(node --version | sed 's/v//')
NODE_MAJOR=$(echo $NODE_VERSION | cut -d. -f1)

if [ "$NODE_MAJOR" -lt 18 ]; then
    echo "Node.js版本过低: v$NODE_VERSION"
    echo "需要Node.js 18+，请先升级Node.js"
    echo ""
    echo "升级方法:"
    echo "  # 方法1: 使用nvm"
    echo "  curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash"
    echo "  source ~/.bashrc"
    echo "  nvm install 18"
    echo "  nvm use 18"
    echo ""
    echo "  # 方法2: 使用yum安装NodeSource"
    echo "  curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -"
    echo "  sudo yum install -y nodejs"
    exit 1
fi

echo "Node.js版本: v$NODE_VERSION"
echo ""

echo "=== 安装依赖 ==="
cd "$FRONTEND_HOME"
npm install

echo ""
echo "=== 构建项目 ==="
npm run build

echo ""
echo "=== 验证构建产物 ==="
if [ -d "dist" ]; then
    echo "构建成功！"
    echo "dist目录大小: $(du -sh dist | cut -f1)"
else
    echo "构建失败！dist目录不存在"
    exit 1
fi

echo ""
echo "======================================"
echo "    前端构建完成!"
echo "======================================"
echo ""
echo "部署方式:"
echo "1. 将 dist 目录内容复制到 Nginx 静态目录"
echo "   cp -r dist/* /usr/share/nginx/html/"
echo ""
echo "2. 配置Nginx反向代理:"
echo "   location /phonebiz {
       proxy_pass http://localhost:8080/phonebiz;
       proxy_set_header Host \$host;
       proxy_set_header X-Real-IP \$remote_addr;
   }"
echo ""
echo "3. 启动开发服务器进行测试:"
echo "   npm run dev"
echo "   访问地址: http://localhost:3000"
echo "======================================"
