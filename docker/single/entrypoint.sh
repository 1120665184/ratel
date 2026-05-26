#!/bin/sh
set -e

# 启动 nginx（后台运行）
nginx -g 'daemon off;' &
NGINX_PID=$!

# 启动后端
java -jar /app/gwsu.jar &
JAVA_PID=$!

# 优雅退出
shutdown() {
    echo "收到退出信号，正在停止服务..."
    kill $JAVA_PID 2>/dev/null
    kill $NGINX_PID 2>/dev/null
    wait $JAVA_PID 2>/dev/null
    wait $NGINX_PID 2>/dev/null
    echo "服务已停止"
    exit 0
}

trap shutdown SIGTERM SIGINT SIGQUIT

# 等待任一进程退出
wait -n $NGINX_PID $JAVA_PID

# 如果有一个进程退出，停止另一个
shutdown
