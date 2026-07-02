#!/bin/sh
set -e

# 确认 Playwright 浏览器环境
if [ -d "${PLAYWRIGHT_BROWSERS_PATH}" ]; then
    echo "Playwright 浏览器目录: ${PLAYWRIGHT_BROWSERS_PATH}"
    ls "${PLAYWRIGHT_BROWSERS_PATH}"
else
    echo "警告: PLAYWRIGHT_BROWSERS_PATH=${PLAYWRIGHT_BROWSERS_PATH} 不存在，Playwright 将尝试运行时下载浏览器"
fi

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

trap shutdown TERM INT QUIT

# 等待任一进程退出（循环轮询，兼容 dash）
while kill -0 $NGINX_PID 2>/dev/null && kill -0 $JAVA_PID 2>/dev/null; do
    sleep 1
done

# 如果有一个进程退出，停止另一个
shutdown
