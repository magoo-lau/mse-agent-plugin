---
name: mse-conventions
description: >
  mse 框架规范问答与技能路由。当用户询问 mse / mseapp 的架构约定、模块命名规范、积木式组装
  原则、Spring Boot SPI 接入标准、事务双注解规范、RPC 契约写法、基座能力/API 消费方式、
  权限点号段与端口分配、微服务与单体的区别取舍，或不确定该用哪个 mse 技能时，使用本技能。
---

# mse 框架规范总览（问答入口 + 技能路由）

本技能位于 `<插件根>/skills/shared/mse-conventions/`，插件根 = 本文件目录的上两级。规范全文见插件根 `references/conventions.md`（先读它再回答细节问题），基座 API 使用见 `references/base-api.md`，资源分配表见 `references/alloc-tables.md`。

## 回答规范问题的依据（速查）

1. **两种形态**：微服务（应用名 `mse-<biz>`，boot + Nacos + Dubbo）与聚合单体（名字含 monolith，`mse.rpc.mode=local`，默认仅需 MySQL/Redis）。MQ 与形态正交：两种形态都可按需加 `mq-starter` 启用 RocketMQ（不配则事件降级 noop）。
2. **命名**：业务模块 `<biz>-boot/-service/-api/-ui`；启动类包 `vip.mseapp.boot.<biz>`；业务包名 `vip.mseapp.<biz>`。
3. **SPI 标准**：每个 service 积木经 `META-INF/spring/...AutoConfiguration.imports` 装配两个自动配置类；启动器纯 `@SpringBootApplication`。
4. **事务双注解**：写操作 `@Transactional(rollbackFor = Exception.class)`；跨库全局入口只用 `@GlobalTransactional`（禁叠加 @Transactional）。
5. **红线**：业务/示例代码不进基座；不裁剪底座；全程不改 mse-framework（网关路由留档 yaml 与 mse-ui `builtinMicroApps` 兜底登记两类登记式配置数据除外）。
6. **基座能力消费**：`@RpcReference` + `starter-bridge`；可消费契约清单、上下文工具、基座 HTTP 端点、wujie 协议见 `references/base-api.md`。
7. **资源分配**：新业务先查 `references/alloc-tables.md`（端口/权限号段/网关前缀），用后登记。

## 技能路由（按需求分发，不要在本技能内替做具体工作）

| 用户需求 | 用哪个技能 |
| --- | --- |
| 新建微服务业务工程 | micro-create-project |
| 新建单体聚合工程 / 单体加业务 | mono-create-project / mono-aggregate-module |
| 加网关路由 / 白名单 | micro-gateway-route |
| 服务 Nacos 专属配置 | micro-nacos-config |
| Dockerfile / k8s 部署物 | micro-deploy-artifacts |
| 加一张表 + CRUD 接口 | gen-crud |
| 跨服务 RPC 契约定义/消费 | gen-rpc-contract |
| 三方言 SQL / 权限点 | gen-sql |
| 新建前端子应用 | gen-ui-subapp |
| 子应用业务页面 | gen-ui-page |
| 租户隔离（独立库/分表/字段隔离）/ 跨库事务 | gen-tenant-db |
| 规范合规检查 | review-conventions |

回答问题时先判断用户处于微服务形态还是单体形态——两者的差异点（boot/Nacos/Dubbo 有无、RPC local/dubbo、配置位置）见 `references/conventions.md` §5/§6。
