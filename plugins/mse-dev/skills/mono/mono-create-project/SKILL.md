---
name: mono-create-project
description: >
  按 mse 框架规范创建单体聚合工程（mse.rpc.mode=local，仅依赖 MySQL/Redis，无 Nacos/Dubbo）。
  当用户要新建单体工程、单体版业务系统、轻量化部署、开发环境零中间件成本运行全部业务时使用；
  与 micro-create-project（微服务形态）相对。
---

# mono-create-project —— 创建单体聚合工程

模板：`<插件根>/templates/mono/`；清单：`references/mono-checklist.md`；分配表：`references/alloc-tables.md`。

## 第 1 步：收集参数（AskUserQuestion）

| 参数 | 说明 | 默认 |
| --- | --- | --- |
| biz | 主业务名（占位/派生用） | 必填 |
| appName | 应用名 | mse-monolith-\<biz\>（也可 mse-monolith） |
| port | 服务端口 | 8080（避开 alloc-tables 已用后端端口） |
| description | 工程描述 | \<biz\> 单体聚合 |
| mseVersion | 基座版本 | 1.0.0-SNAPSHOT |
| 目标目录 | 工程位置 | mse-modules/mse-\<biz\>-monolith 或用户指定 |

## 第 2 步：渲染

```bash
node <插件根>/scripts/render.mjs <插件根>/templates/mono <目标目录> \
  --biz=<biz> --app-name=<appName> --port=<port> --description="<描述>" \
  [--module-dir=<目标目录相对仓库根路径>]
```

## 第 3 步：聚合业务积木

在 pom 的「聚合业务积木」注释占位段为每个要聚合的业务加一条 `<biz>-service` 依赖（**oa 用 oa-service 聚合件**）。前置：mse 基座已 `mvn install -DskipTests` 发布到本地仓库、各业务工程已 `mvn install`。聚合动作到此为止——无需扫描配置、无需启动类改动（SPI 自动装配）。

## 第 4 步：初始化与验证

1. 初始化数据库：零手工——master 库（默认=缺省共享租户库 mse_tenant）首启自动建库，基座结构与种子随启动自动应用；
2. 业务 SQL 免手工执行：每个已聚合业务积木 jar 内 `db/<方言>/` 版本化脚本自动应用——`V…_<biz>__init.sql`（头指令 -- mse-target: master，业务表）随启动 auto-migrate 应用主库（模板 application.yaml 已开 `mse.tenant.schema.auto-migrate: true`，失败阻断启动），`V…_<biz>__base.sql`（头指令 -- mse-target: base，子应用注册 + 页面菜单 + 权限点，幂等）经 starter-bridge 推送 mse-system 车队重放到租户库（共享主库+各独立租户库各持副本，周期重推自愈）；
3. `mvn install -DskipTests` 通过；
4. 启动 `MonolithApplication`（纯 @SpringBootApplication），全链路人工回归：登录 → 系统管理 → 各业务接口（@SaCheckPermission 生效）；租户端业务页面菜单需在「套餐管理」勾选；
5. 有子前端时 gen-ui-subapp 的 proxy 指向单体端口（默认 8080）。

## 红线

- 默认无 Nacos 配置导入、无 Nacos/Dubbo/Seata 依赖；`mse.rpc.mode: local` + `mse.tenant.enabled: true`（模板已配，勿动）。
  MQ 例外且与形态无关：业务确实需要 RocketMQ 时，pom 加 `mq-starter` + yaml 放开 rocketmq 段即可（不配则事件降级 noop，启动不受影响）。
- 基座 service-system/tenant/auth 本地实现存在时，starter-bridge 的 RPC 回退自动让位——业务积木代码与微服务形态完全一致，**不要为单体复制/改写业务积木代码**。
- 信创切换：`--spring.profiles.active=dm|kingbase`（application.yaml 已含 profile 段）。
