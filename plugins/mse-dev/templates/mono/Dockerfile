# syntax=docker/dockerfile:1
# =============================================================================
# {{appName}} 聚合单体镜像（多阶段构建，源码自包含，无需宿主机预装 Maven/JDK）；
# ARG MODULE/PORT 按工程渲染注入
#
# 构建（context 必须是仓库根目录，能同时看到业务工程与基座模块）：
#   docker build -f {{moduleDir}}/Dockerfile -t mse/{{appName}}:1.0.0 .
# =============================================================================

ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

# ---- 阶段 1：Maven 构建 ----
FROM ${MAVEN_IMAGE} AS builder
ARG MODULE={{moduleDir}}
ARG MAVEN_ARGS=""
WORKDIR /build

COPY . .
# .m2 走 BuildKit 缓存挂载：源码变动不触发依赖重新下载
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests ${MAVEN_ARGS} package -pl ${MODULE} -am

# Spring Boot tools jarmode 分层解包：依赖层与应用层分离，业务代码变动时命中缓存只换最上层
RUN cp ${MODULE}/target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination /extracted

# ---- 阶段 2：运行时 ----
FROM ${RUNTIME_IMAGE}
ARG PORT={{port}}

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

RUN groupadd -r -g 1001 app && useradd -r -u 1001 -g app -s /usr/sbin/nologin app

WORKDIR /app
COPY --from=builder --chown=app:app /extracted/dependencies/ ./
COPY --from=builder --chown=app:app /extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /extracted/application/ ./
USER app
EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
