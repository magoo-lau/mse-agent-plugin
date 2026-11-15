# 单体聚合工程清单（mono-checklist）

> 蓝本 `mse-sample/mse-monolith/`，模板位于插件根 `templates/mono/`。单体 = 多个积木一个进程，`mse.rpc.mode=local`，默认仅需 MySQL/Redis，**无 boot/Nacos/Dubbo 配置导入**；MQ 与形态无关——需要 RocketMQ 时 pom 加 `mq-starter` + yaml 放开 rocketmq 段即可（不配则事件降级 noop）。

## 新建独立单体工程

```
<工程目录>/
├── pom.xml                  【P】独立单模块 pom（packaging jar）：parent 指向基座 mse-starter-parent
│                                  （<relativePath/>，收编 Java 21/插件/BOM/仓库）+
│                                  依赖 mono-starter（聚合基座三积木 + 全部装配件 + actuator + 三方言驱动；
│                                  starter-bridge 随基座本地实现让位）+
│                                  【聚合业务在此追加 <biz>-service 依赖】+
│                                  spring-boot-maven-plugin repackage
├── Dockerfile               【P】ARG MODULE=<工程相对仓库根路径>、ARG PORT
├── k8s/monolith.yaml        【P】Deployment+Service，name=<应用名>；探针 exec+curl 带 X-Actuator-Token（模板已内置）
└── src/main/
    ├── java/vip/mseapp/boot/monolith/MonolithApplication.java   纯 @SpringBootApplication
    └── resources/application.yaml  【P】见下
```

## application.yaml 关键项（缺一不可）

```yaml
server.port: <端口>
spring.application.name: <应用名>
spring.datasource.dynamic:                  # master 必配；lazy: true、strict: false
  primary: master
mse.rpc.mode: local                         # 单体标识（勿放 Nacos）
mse.tenant.enabled: true
mse.tenant.schema.auto-migrate: true        # 聚合业务 jar 内 db/<方言>/ 版本化脚本（mse-target: master）随启动自动建表（失败阻断启动）
mse.actuator.auth.token: ${MSE_ACTUATOR_TOKEN:}   # actuator 令牌鉴权（空=关闭；K8s 探针同源）
mybatis-plus: id-type=assign_id + 逻辑删除 deleted
sa-token: token-name=${MSE_TOKEN_NAME:Authorization} 等
---
dm / kingbase profile 段（数据源 + mse.mybatis.db-type: dm|kingbase_es）
```

独立库租户（`@TenantDatasource` 数据源路由与跨库事务）见 gen-tenant-db 技能。聚合业务的服务身份由各积木自带（ServiceAutoConfiguration 标 `@TenantServiceModule("mse-<biz>")`），monolith 无需配 `mse.tenant.service-mappings`。

## 聚合新业务积木（也可用 mono-aggregate-module skill）

1. pom 加一条 `<biz>-service` 依赖（**oa 用 oa-service 聚合件**，不引散件）；
2. 前置：该业务工程已 `mvn install`；
3. SQL 免手工执行：业务积木 jar 内 `db/<方言>/` 版本化脚本自动应用——`V…_<biz>__init.sql`（头指令 -- mse-target: master，业务表）随启动 auto-migrate 应用主库，`V…_<biz>__base.sql`（头指令 -- mse-target: base，子应用注册+页面菜单+权限点）经 starter-bridge 推送 mse-system 车队重放到租户库（共享主库+各独立租户库各持副本，周期重推自愈）；
4. 单体基座本地实现存在时，bridge 的 RPC 回退自动让位（业务内 @RpcReference 本地直调）；
5. `mvn install -DskipTests` 后启动验证：登录 → 业务接口 → `@SaCheckPermission` 生效；
6. 业务需要文件存储（附件/图片/素材上传）时按 `references/fs-integration.md` 接入——mse-fs 优先（聚合 fs-service 后自动切换）、本地降级，勿自造落盘逻辑。

## 核对清单

- [ ] 无 nacos 配置导入 / 无 nacos 依赖 / 无 dubbo 依赖
- [ ] `mse.rpc.mode: local` 已配置
- [ ] 启动类纯 @SpringBootApplication（无 scanBasePackages/@MapperScan）
- [ ] 聚合的每个业务 jar 内 db/<方言>/ 版本化脚本（mse-target 头区分 master 业务表 / base 基座接入）已自动应用（业务表在业务库，权限点/页面菜单经推送进基座库全局表；查 mse_<biz>_schema_history 分册账本确认）
- [ ] 前端子应用 proxy `/api` 指向单体端口（默认 8080）
- [ ] `mvn install -DskipTests` 通过；启动后 health、登录、业务接口全链路可用
