# 第一阶段：获取 JRE
FROM eclipse-temurin:25-jre-alpine-3.23 AS jre-source

# 第二阶段：基于 nginx 官方镜像构建
FROM nginx:stable-alpine

# 复制 Java 运行时
COPY --from=jre-source /opt/java/openjdk /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# 创建应用目录
WORKDIR /app

# 复制后端 jar
COPY business/application/single/gwsu/target/gwsu.jar /app/gwsu.jar

# 复制前端静态文件
COPY web/apps/gwsu-main/dist /usr/share/nginx/html/main
COPY web/apps/gwsu-sub-system/dist /usr/share/nginx/html/sub-system
COPY web/apps/gwsu-sub-security/dist /usr/share/nginx/html/sub-security

# 复制 nginx 配置（覆盖默认配置）
COPY docker/nginx/gwsu.conf /etc/nginx/conf.d/default.conf

# 复制启动脚本
COPY docker/single/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 暴露 nginx 端口
EXPOSE 80

ENTRYPOINT ["sh", "/app/entrypoint.sh"]
