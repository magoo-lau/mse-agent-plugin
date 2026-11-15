---
name: micro-create-project
description: >
  按 mse 框架规范创建微服务业务工程（mse-modules/mse-<biz>：可选 xxx-api 契约 + xxx-service
  业务积木（含 jar 内版本化迁移脚本）+ xxx-boot 启动器 + Nacos 配置留档）。当用户要新建业务工程、
  新建后端微服务、创建 mseapp 业务项目、按脚手架生成工程时使用；会交互式收集业务名、包名、端口、
  权限号段等参数后用模板渲染生成。
---

# micro-create-project —— 创建微服务业务工程

模板：`<插件根>/templates/micro/`（插件根 = 本文件目录的上两级）；渲染脚本 `<插件根>/scripts/render.mjs`；生成前先读 `references/micro-checklist.md` 与 `references/alloc-tables.md`。

## 第 1 步：收集参数（用 AskUserQuestion，必须先问再生成）

| 参数 | 说明 | 默认/建议 |
| --- | --- | --- |
| biz（必填） | 业务名，小写英文（如 mp、crm），决定模块/包/应用/表前缀 | —— |
| package | 业务包名 | vip.mseapp.\<biz\> |
| description | 工程一句话描述 | \<biz\> 业务 |
| port | 后端端口 | 查 alloc-tables 取下一个可用（8098+） |
| 权限号段基点 | sys_permission id 基点（示例权限用 4 个：list/create/update/delete） | 查 alloc-tables 选未占号段（如 4400 → perm1..perm4 = 4401..4404） |
| 子应用注册 id | sys_micro_app.id（接入子前端时必填） | 查 alloc-tables 取下一个可用（11+） |
| 菜单号段基点 | sys_menu type=3 页面菜单 id 基点（示例菜单用 3 个：分组目录+2 页面） | 查 alloc-tables 选未占段（如 500 → menu1..menu3 = 500..502） |
| uiPort / uiTitle | 子前端 dev 端口与显示名（mse-target: base 基座接入脚本模板引用：dev 入口与侧边栏标题） | uiPort 查 alloc-tables（4020+）；uiTitle=业务中文名。**不接子前端时可删 `V…_<biz>__base.sql` 后不传** |
| 是否 xxx-api 契约模块 | 需要对外 RPC 契约才保留 | 保留（--no-api 可去掉） |
| 是否需要子前端 | 是则生成后接 gen-ui-subapp | 询问 |
| mseVersion | 基座版本 | 1.0.0-SNAPSHOT |
| 目标目录 | 工程位置 | mse-modules/mse-\<biz\>（--module-dir） |

## 第 2 步：渲染

```bash
node <插件根>/scripts/render.mjs <插件根>/templates/micro <目标目录> \
  --biz=<biz> --port=<port> --perm1=<n1> --perm2=<n2> --perm3=<n3> --perm4=<n4> \
  --description="<描述>" [--package=<包名>] [--mse-version=<版本>] [--no-api] \
  [--app-id=<micro_app id> --menu1=<目录id> --menu2=<页面id> --menu3=<页面id> --ui-port=<port> --ui-title=<名>]
```

`--app-id/--menu1..3/--ui-port/--ui-title` 供 jar 内 `db/<方言>/V{{sqlDate}}_01_{{biz}}__base.sql`（基座接入版本化脚本，头指令 `-- mse-target: base`，模板文件名含 `{{sqlDate}}` 日期占位符、默认渲染当天，可 `--sql-date` 覆盖）使用：接入子前端（或确定 SQL 落库接入）时必填；不接子前端时删除 base 脚本后可不传。

脚本会自动派生包名/应用名/网关前缀/启动类包，渲染后扫描残留占位符并报错。目标目录已存在时先与用户确认。

## 第 3 步：样例代码去留

模板含一个可编译运行的样例 CRUD（`<Biz>Item`：entity/mapper/service/controller + 权限点 + 可选 RPC 契约），作为规范参考。**用户明确不要样例时**删除：service 内 item 相关四类文件 + `V…_{{biz}}__init.sql` 中 `{{biz}}_item` 表 + `V…_{{biz}}__base.sql` 中 4 条权限点（SPI 文件与自动装配类保留，它们是骨架本体）。

## 第 4 步：联动与验证（缺一不可）

1. 网关路由：接 micro-gateway-route 技能，在 `mse-framework/docker/nacos/config/mse-gateway.yaml` 登记 `Path=/<biz>/** → lb://mse-<biz>`（登记式配置数据，允许的基座改动之一，见 `references/conventions.md` §2）。
2. Nacos 配置导入：接 micro-nacos-config 技能（`CONFIG_DIR=<工程>/docker/nacos/config sh mse-framework/docker/nacos/init-configs.sh`）。
3. SQL 免手工执行：业务表脚本（`<biz>-service/src/main/resources/db/<方言>/V…_<biz>__init.sql`，头指令 `-- mse-target: master`）随服务启动自动应用（`mse.tenant.schema.auto-migrate: true` 默认已开，失败阻断启动）；基座接入脚本（`db/<方言>/V…_<biz>__base.sql`，头指令 `-- mse-target: base`，子应用注册 + 页面菜单 + 权限点，幂等）由 starter-bridge 就绪即推 mse-system 按账本应用（周期重推自愈）。租户端页面菜单应用后还需在平台端「套餐管理」勾选才对该租户可见。
4. 编译验证：前置 mse 基座已 `mvn install -DskipTests` 发布到本地仓库；然后 `cd <目标目录> && mvn install -DskipTests`。
5. 资源登记：把端口/权限号段/子应用注册 id/菜单号段/网关前缀写入 `references/alloc-tables.md`。
6. 需要子前端时接 gen-ui-subapp；第一个业务模块如需替换样例接 gen-crud；需要消费基座数据（用户/组织/租户）时接 gen-rpc-contract（可消费契约清单见 `references/base-api.md`）。

## 红线

- 启动类必须在 `vip.mseapp.boot.<biz>`，纯 @SpringBootApplication（模板已保证，勿"优化"）。
- 不得修改 mse-framework 任何文件（mse-gateway.yaml 路由留档与 mse-ui `builtinMicroApps` 兜底登记两类登记式配置数据除外，见 `references/conventions.md` §2）。
- boot 不引基座 service-system/tenant/auth（鉴权走 micro-starter 内置的 starter-bridge 契约消费）。
- MQ/Seata 默认关闭（pom 注释 + `spring.datasource.dynamic.seata: false`），启用时三处同步：service 依赖、boot 依赖、Nacos 配置 + SQL undo_log。
