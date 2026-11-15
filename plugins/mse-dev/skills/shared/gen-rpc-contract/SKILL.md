---
name: gen-rpc-contract
description: >
  按 mse 框架规范生成 RPC 契约：xxx-api 模块（接口 + Serializable DTO）、@RpcService 实现、
  自动装配类 @Bean + @ConditionalOnMissingBean 注册、消费方 @RpcReference 注入；含消费基座
  契约（api-system/api-tenant + starter-bridge）的接法。当用户要跨服务调用、定义对外
  契约、其他服务查本服务数据、消费基座用户/租户数据时使用。
---

# gen-rpc-contract —— RPC 契约定义与消费

参考实现：`mse-sample/mse-demo/demo-api`（契约）+ `demo-service/rpc/RemoteDemoOrderServiceImpl`（实现）。规范依据：`references/conventions.md` §7。

## A. 本服务对外提供契约

1. **契约模块** `<biz>-api`（工程没有时按 demo-api 建：parent + 仅依赖 common-core；包 `vip.mseapp.<biz>.api`）：
   - 接口 `Remote<Xxx>Service.java`：纯业务接口；javadoc 注明实现方与消费方式；**方法不携带 tenantId**（租户上下文经 RPC 隐式透传）。
   - DTO `dto/<Xxx>DTO.java`：`implements Serializable`、`serialVersionUID = 1L`、手写 getter/setter；与实体解耦，只放跨服务需要的字段。
2. **实现** `service` 工程内 `rpc/Remote<Xxx>ServiceImpl.java`：标 `@RpcService(Remote<Xxx>Service.class)`；经 Mapper/Service 实现逻辑。
3. **注册**：`<Biz>ServiceAutoConfiguration` 加 `@Bean + @ConditionalOnMissingBean(Remote<Xxx>Service.class)` 工厂方法——接入方声明同契约 Bean 即可整体替换默认实现。
4. **消费方**：pom 依赖 `<biz>-api`，字段 `@RpcReference private Remote<Xxx>Service xxxService;`。单体 local 本地直调、微服务 dubbo 远程调用，**消费方代码零改动**。
5. **调参写法（生成代码同此约定）**：`@RpcReference`/`@RpcService` 外层**无 dubbo 扁平属性**（不要生成 `@RpcReference(timeout=...)`），超时/重试/负载均衡等一律走嵌套镜像注解——`@RpcReference(dubbo = @RpcReference.Dubbo(timeout = 60_000, retries = 3))`、`@RpcService(dubbo = @RpcService.Dubbo(version = "1.0", timeout = 8000))`（分别为 @DubboReference/@DubboService 全量属性镜像，缺省 timeout=5000/check=false）。方法级配置在 `dubbo().methods` 直用 Dubbo 原生注解——`methods = {@Method(name = "slowCall", timeout = 180_000)}`（import org.apache.dubbo.config.annotation.Method；dubbo 经 common-core 编译期传递，无需模块自加依赖）。

## B. 本服务消费基座能力

boot 引 micro-starter（内置 `starter-bridge`）时，直接引 `api-system`/`api-tenant` 契约并用 `@RpcReference` 消费（鉴权数据源/租户路由/操作日志由 bridge 自动提供 RPC 回退实现；组织/用户查询无回退，需自行 `@RpcReference RemoteOrgService`/`RemoteUserService`）。可消费契约清单、上下文工具与基座 HTTP 端点见 `references/base-api.md`。**不要**把基座 service-system/tenant/auth 打进业务 boot。

## C. 本服务能力开放（接 mse-open，业务零改动进目录）

1. **机器语义契约**：`<biz>-api` 契约方法签名统一 `String 方法名(String requestJson)`（请求/返回 JSON 字符串）；操作者显式携带（如 `operatorUserId`，机器链路无登录态）、不依赖 UserContext；租户经上下文隐式透传，契约不传 tenantId。实现标 `@RpcService` + `@Bean + @ConditionalOnMissingBean` 注册。
2. **自注册目录**：依赖 mse-open 的 `open-api` 契约 jar，声明一个 `OpenApiContributor` Bean（code/name/path/httpMethod/target 描述清单，按 **Bean 名**判重防多积木互相让位）。
3. **生效**：应用就绪后 open-api 装配自动推送 mse-open 目录（幂等 upsert + 周期重推自愈），**无需任何 SQL 种子**；管理端「API 目录」可启停（即时生效）。参考实现：mse-oa（oa-api + OaOpenApiContributor）、mse-ai（契约复用，仅 Contributor）、mse-fs/mse-mp/mse-data 各自 `*OpenApiContributor`。

## 红线

- 跨服务 RPC 唯一合法依赖是 mse-api 系契约 jar；禁止跨服务引他人 service 实现模块。
- DTO 禁用 Lombok 之外的序列化框架字段/不可序列化类型（LocalDateTime 等注意兼容）。
- `@GlobalTransactional` 相关 bean 必须组件扫描注册，不经 @Bean 工厂方法（Seata 2.6 限制）。

## 验证

提供方：`mvn install -DskipTests` 后启动，确认契约导出（dubbo 日志/local 直调）。消费方：写一个查询端点实测返回；微服务形态下两服务都需在 Nacos 注册。
