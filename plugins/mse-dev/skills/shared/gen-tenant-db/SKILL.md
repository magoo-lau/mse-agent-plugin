---
name: gen-tenant-db
description: >
  按 mse 框架规范接入租户隔离：实体 @TenantDatasource 标注、平台端「租户数据源/隔离策略」
  界面化管理（独立库/共享库分表/字段隔离三种模式，静态 yaml 双轨兼容）、租户数据源绑定开通、
  跨库写入只用 @GlobalTransactional 的事务模式。当用户要租户独立库/数据隔离、混合租户路由、
  共享库分表、跨库一致性事务时使用。
---

# gen-tenant-db —— 独立库租户接入

参考实现：`mse-sample/mse-demo`（DemoOrder + placeOrder + tenant2 数据源）。机制：隔离配置按（租户×服务）解析——平台端「隔离策略」维护的 `sys_tenant_service_config` 优先，`sys_tenant.ds_key` 作为全局默认兜底（存量租户自动兼容）；服务身份由各业务积木自带（ServiceAutoConfiguration 标 `@TenantServiceModule("mse-<biz>")`，单体聚合零 yaml 生效，`mse.tenant.service-mappings` 为逃生舱）；命中独立库时标注 `@TenantDatasource` 的实体读写自动路由到专属数据源，未配置回落本服务主库（strict=false），等价共享库语义。

## 接入步骤

1. **实体标注**：需要隔离的实体加 `@TenantDatasource`（`vip.mseapp.core.annotation`）；独立库与主库**表结构一致**（均含 tenant_id 列），表结构以 jar 内 `db/<方言>/` 版本化脚本（mse-target: master）为准，租户库由初始化/懒初始化克隆+迁移自动就绪。
2. **数据源与隔离策略维护（界面化）**：平台端「平台管理 → 租户数据源」新增数据源（key/库模式 shared 共享库-多租户绑定 / dedicated 独立库-一库一租户/所属服务/方言/连接信息）→「测试连接」→「初始化」（自动建库 + 按 db/<方言>/ 版本化脚本（mse-target: master）克隆+迁移业务表与 undo_log，自动就绪）→ 保存即运行时注册，**无需改配置/重启**。隔离粒度在「平台管理 → 隔离策略」按 (租户×服务) 配置三种模式：INDEPENDENT_DB 独立库 / SHARED_TABLE_SPLIT 共享库分表（逻辑表+租户后缀）/ SHARED_FIELD 字段隔离，表级策略可覆盖服务级默认；首访命中未初始化租户库会自动懒初始化（克隆+迁移）。机制见 docs/tech/service/service-platform.md「租户数据源与多模式隔离」。
   - 静态方式仍兼容：单体 `application.yaml` / 微服务 Nacos 留档 yaml 的 `spring.datasource.dynamic.datasource.<ds_key>` 段（模板 tenant2 示例），同 key 时动态注册优先；
   - `lazy: true`（库未建不阻塞启动）+ `strict: false`（未注册回落平台库）保持默认。
3. **开通租户**：平台端租户管理开通时**必选**数据源绑定（下拉来自「租户数据源」，dedicated 已占用不可选）。
4. **事务模式（红线，见 `references/conventions.md` §4）**：
   - 同库写操作：`@Transactional(rollbackFor = Exception.class)`；
   - **一个方法内跨多库路由（写主库 + 写租户库）：禁止 @Transactional**（Spring 本地事务在方法进入时绑定连接，先于数据源路由决策），只用 `@GlobalTransactional(rollbackFor = Exception.class)`（Seata 关闭/缺失时被忽略）；
   - `@GlobalTransactional` 所在类必须 `@Service` 组件扫描注册，**不能**经 AutoConfiguration @Bean 工厂方法注册（Seata 2.6 预扫描静默跳过工厂方法 bean）；
   - 启用 Seata：无需改 pom（Seata 已随 micro-starter 内置）、共享配置 MSE_SEATA_ENABLED=true、Nacos 留档删 `spring.datasource.dynamic.seata: false`、所有写入库有 undo_log。

## 验证（参考 demo 全链路）

1. 共享库租户：写 → 落 master；独立库租户（ds_key 命中）：写 → 落 `mse_tenant_<n>`；
2. 全局事务回滚：跨库方法中写后抛错 → 两库均无脏数据（undo_log 回放）；
3. 未开独立库的租户行为不变（回落 master）。
