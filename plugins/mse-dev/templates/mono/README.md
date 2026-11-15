# {{appName}} —— {{description}}（单体聚合）

独立单体聚合工程（积木式组装），`mse.rpc.mode=local`，默认仅需 MySQL/Redis，无 Nacos/Dubbo。基座基础能力（RBAC/多租户/认证/系统管理）开箱即得；**聚合业务 = 在 pom 加一条 `<biz>-service` 依赖**（SPI 自动装配）。MQ 与形态无关：需要 RocketMQ 时 pom 加 `mq-starter` + 放开 yaml 的 rocketmq 段即可（不配则事件降级 noop）。

## 关键参数

- 应用名：`{{appName}}`；端口：`{{port}}`
- 聚合的业务积木：见 pom 注释占位段（每业务一条依赖）
- 启动类：`vip.mseapp.boot.monolith.MonolithApplication`（纯 @SpringBootApplication）

## 本地开发

```bash
# 前置：mse 基座已发布到本地仓库（基座根目录 mvn install -DskipTests）；被聚合业务工程已各自 mvn install
mvn install -DskipTests

# 初始化：仅基座 SQL（01/02）需手工执行；业务 SQL 免手工——
#   业务表（各业务 jar 内 db/tenant 版本化脚本）随启动自动应用（mse.tenant.schema.auto-migrate 已开，失败阻断启动）；
#   基座接入（各业务 jar 内 db/base 脚本：子应用注册+页面菜单+权限点，幂等）经 starter-bridge 推送 mse-system 车队重放到租户库（共享主库+各独立租户库各持副本，周期重推自愈）。
```

启动后全链路验证：`http://localhost:{{port}}` —— 登录（auth）→ 系统管理 → 各业务接口（@SaCheckPermission 生效）。

## 信创数据库

启动加 `--spring.profiles.active=dm` 或 `kingbase`（application.yaml 已含 profile 段）；业务迁移脚本为各业务 jar 内 `db/tenant|base` 的 dm/kingbase 方言目录版本，同样自动应用。

## 部署

- 镜像：`docker build -f Dockerfile -t mse/{{appName}}:1.0.0 .`（context = 仓库根）
- k8s：`k8s/monolith.yaml`

## 切微服务形态

单体是开发/轻量部署形态；业务上线按微服务组织时，用各业务自己的 `<biz>-boot`（Nacos + Dubbo + 网关路由），业务积木代码零改动。
