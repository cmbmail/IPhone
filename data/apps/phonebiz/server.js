const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = 80;
const FRONTEND_DIR = path.join(__dirname, 'frontend', 'dist');
const BACKEND_HOST = '127.0.0.1';
const BACKEND_PORT = 8081;
const BACKEND_PREFIX = '/phonebiz';

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.txt': 'text/plain; charset=utf-8'
};

function getContentType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  return MIME_TYPES[ext] || 'application/octet-stream';
}

function proxyRequest(req, res) {
  const options = {
    hostname: BACKEND_HOST,
    port: BACKEND_PORT,
    path: req.url,
    method: req.method,
    headers: req.headers
  };

  const proxy = http.request(options, (backendRes) => {
    res.writeHead(backendRes.statusCode, backendRes.headers);
    backendRes.pipe(res);
  });

  proxy.on('error', (err) => {
    console.error('Proxy error:', err);
    res.writeHead(503, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ 
      code: 503, 
      message: '后端服务不可用',
      error: err.message 
    }));
  });

  req.pipe(proxy);
}

function serveStatic(req, res) {
  const parsedUrl = url.parse(req.url);
  let pathname = parsedUrl.pathname;

  if (pathname === '/' || pathname === '') {
    pathname = '/index.html';
  }

  let filePath = path.join(FRONTEND_DIR, pathname);

  const extname = path.extname(filePath);
  if (!extname) {
    filePath = path.join(FRONTEND_DIR, 'index.html');
  }

  fs.exists(filePath, (exists) => {
    if (!exists) {
      filePath = path.join(FRONTEND_DIR, 'index.html');
    }

    fs.readFile(filePath, (err, data) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('服务器内部错误');
        return;
      }

      const contentType = getContentType(filePath);
      res.writeHead(200, { 
        'Content-Type': contentType,
        'Cache-Control': 'no-cache, no-store, must-revalidate'
      });
      res.end(data);
    });
  });
}

const server = http.createServer((req, res) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);

  if (req.url.startsWith(BACKEND_PREFIX)) {
    proxyRequest(req, res);
  } else {
    serveStatic(req, res);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('======================================');
  console.log('  PhoneBiz Web Server');
  console.log('======================================');
  console.log(`服务已启动: http://0.0.0.0:${PORT}`);
  console.log(`前端路径: ${FRONTEND_DIR}`);
  console.log(`后端代理: http://${BACKEND_HOST}:${BACKEND_PORT}${BACKEND_PREFIX}`);
  console.log('======================================');
});

server.on('error', (err) => {
  if (err.code === 'EACCES') {
    console.error(`权限错误: 无法绑定到端口 ${PORT}`);
    console.error('请使用sudo运行: sudo node server.js');
    process.exit(1);
  }
  if (err.code === 'EADDRINUSE') {
    console.error(`端口 ${PORT} 已被占用`);
    process.exit(1);
  }
  console.error('服务器错误:', err);
});
