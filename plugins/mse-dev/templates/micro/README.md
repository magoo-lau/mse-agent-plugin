# mse-{{biz}} —— {{description}}

独立业务工程（积木式组装，微服务形态），仅通过 BOM 消费基座发布件，不进根 pom 聚合。

## 模块

| 模块 | 说明 |
| --- | --- |
| {{biz}}-api | 对外 RPC 契约（接口 + Serializable DTO） |
| {{biz}}-service | 业务积木（纯 jar，SPI 自装配）：Controller/Service/Mapper 引 jar 即得；jar 内含版本化迁移脚本 `resources/db/<方言>/`（业务表 mse-target: master 启动自动应用 + 基座接入 mse-target: base 自动推送） |
| {{biz}}-boot | 微服务启动器：application.yaml 经 spring.config.import 接 Nacos，mse.rpc.mode=dubbo，网关 `lb://{{appName}}` |

## 关键参数

- 应用名：`{{appName}}`；端口：`{{port}}`；网关前缀：`{{gatewayPrefix}}/**`
- 权限点号段：{{perm1}}–{{perm4}}（`{{biz}}:item:*` 示例权限）
- 包名：`{{package}}`（启动类 `vip.mseapp.boot.{{biz}}`）

## 本地开发

```bash
# 前置：mse 基座已发布到本地仓库（基座根目录 mvn install -DskipTests）
mvn install -DskipTests

# Nacos 写入服务专属配置（本工程留档 docker/nacos/config/{{appName}}.yaml）
CONFIG_DIR=<仓库根>/{{moduleDir}}/docker/nacos/config sh mse-framework/docker/nacos/init-configs.sh

# SQL 免手工执行：
#   业务表（db/mysql/V{{sqlDate}}_01_{{biz}}__init.sql，mse-target: master）随服务启动自动应用（mse.tenant.schema.auto-migrate 默认开启，失败阻断启动）；
#   基座接入（db/mysql/V{{sqlDate}}_01_{{biz}}__base.sql，mse-target: base，子应用注册+页面菜单+权限点，幂等）由 starter-bridge 就绪即推 mse-system，车队重放到租户库（共享主库+各独立租户库各持副本）按各库分册账本应用（周期重推自愈）。

# 启动（注册 Nacos + 自动建表 + 推送基座注册，网关路由 {{gatewayPrefix}}/** → lb://{{appName}}）
```

## 信创数据库

启动加 `--spring.profiles.active=dm` 或 `kingbase`（数据源/方言由共享 mse-micro-db.yaml 提供）；迁移脚本按 jar 内 `db/dm|kingbase/` 方言目录自动扫描应用（模板当前随仓库惯例仅提供 mysql/，信创方言脚本需自行转译补入）。

## 部署

- 镜像：`docker build -f {{biz}}-boot/Dockerfile -t mse/{{biz}}-boot:1.0.0 .`（context = 仓库根）
- k8s：`{{biz}}-boot/k8s/{{biz}}.yaml`（复用基座 configMap mse-config）

## 聚合为单体（可选）

把 `{{biz}}-service` 作为依赖加入聚合单体 pom（参考 `mse-sample/mse-monolith`），SPI 自动装配，`mse.rpc.mode=local` 本地直调，无需 Nacos/Dubbo。
