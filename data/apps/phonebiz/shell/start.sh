#!/bin/bash

APP_NAME="phonebiz-1.0.0-SNAPSHOT.jar"
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
JAR_FILE="$APP_HOME/target/$APP_NAME"
PID_FILE="$APP_HOME/app.pid"
LOG_FILE="$APP_HOME/app.log"
PORT=8081
CONTEXT_PATH="/phonebiz"

check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

kill_port_process() {
    local port=$1
    local pid=$(lsof -ti :$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "端口 $port 被占用，PID: $pid，正在停止..."
        kill -15 $pid 2>/dev/null
        sleep 3
        if ps -p $pid > /dev/null 2>&1; then
            kill -9 $pid 2>/dev/null
        fi
        sleep 1
    fi
}

start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "应用已在运行，PID: $PID"
            echo "访问地址: http://localhost:$PORT$CONTEXT_PATH"
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi

    if check_port $PORT; then
        kill_port_process $PORT
    fi

    if [ ! -f "$JAR_FILE" ]; then
        echo "JAR文件不存在: $JAR_FILE"
        echo "请先编译项目: mvn clean package -DskipTests"
        return 1
    fi

    echo "======================================"
    echo "    PhoneBiz Backend"
    echo "======================================"
    echo "JAR文件: $JAR_FILE"
    echo "端口: $PORT"
    echo "上下文路径: $CONTEXT_PATH"
    echo "======================================"
    echo "Starting $APP_NAME..."

    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
    nohup java $JAVA_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    
    sleep 5
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1 && check_port $PORT; then
        echo "应用启动成功！"
        echo "PID: $PID"
        echo "日志文件: $LOG_FILE"
        echo "访问地址: http://localhost:$PORT$CONTEXT_PATH"
        return 0
    else
        echo "应用启动失败，请检查日志: $LOG_FILE"
        tail -50 "$LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

stop() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "Stopping $APP_NAME, PID: $PID..."
            kill -15 $PID 2>/dev/null
            sleep 3
            
            if ps -p $PID > /dev/null 2>&1; then
                echo "强制停止..."
                kill -9 $PID 2>/dev/null
            fi
            
            rm -f "$PID_FILE"
            echo "应用已停止"
            return 0
        else
            echo "应用已停止"
            rm -f "$PID_FILE"
            return 0
        fi
    else
        echo "PID文件不存在，应用可能未运行"
        return 1
    fi
}

status() {
    echo "======================================"
    echo "    服务状态"
    echo "======================================"
    
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "后端服务: 运行中 (PID: $PID)"
            if check_port $PORT; then
                echo "端口 $PORT: 监听正常"
            else
                echo "端口 $PORT: 未监听"
            fi
        else
            echo "后端服务: 未运行"
            rm -f "$PID_FILE"
        fi
    else
        echo "后端服务: 未运行"
    fi
    
    if check_port 80; then
        echo "Nginx (80端口): 运行中"
    else
        echo "Nginx (80端口): 未运行"
    fi
    
    echo "======================================"
}

restart() {
    echo "重启应用..."
    stop
    sleep 2
    start
}

logs() {
    if [ -f "$LOG_FILE" ]; then
        if [ -n "$2" ]; then
            tail -n $2 "$LOG_FILE"
        else
            tail -f "$LOG_FILE"
        fi
    else
        echo "日志文件不存在: $LOG_FILE"
    fi
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs "$@"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs [lines]}"
        echo "  start   - 启动后端服务"
        echo "  stop    - 停止后端服务"
        echo "  restart - 重启后端服务"
        echo "  status  - 查看服务状态"
        echo "  logs    - 查看日志 (可选: 显示行数)"
        exit 1
        ;;
esac

exit 0
