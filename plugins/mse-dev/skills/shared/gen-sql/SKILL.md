---
name: gen-sql
description: >
  按 mse 框架规范生成业务 SQL 的三方言**版本化迁移脚本**（MySQL / 达梦 DM8 / 金仓 KingbaseES）：
  业务表（BaseEntity 基列 + tenant_id）、sys_permission 权限点、号段冲突校验、Seata undo_log。
  脚本放 <biz>-service 的 jar 内资源目录 db/<方言>/（单层，目标由脚本头指令 -- mse-target: 声明），
  随服务启动自动应用 / 经 starter-bridge 自动推送，免手工执行。当用户要写建表脚本、加权限点、初始化业务库、
  变更表结构、兼容信创数据库时使用。
---

# gen-sql —— 三方言版本化迁移脚本

位置：**业务积木 jar 内资源**（不再是工程级 `sql/` 目录），`<biz>-service/src/main/resources/db/<方言>/`（**单层目录**，无 tenant/base 子目录），目标库由**脚本头指令**声明——

- `-- mse-target: master`（**缺省**，目标=业务主库 + 租户独立库：业务表 + 种子，随租户库克隆进各租户库）
- `-- mse-target: base`（目标=基座共享库：sys_micro_app 注册 + sys_menu type=3 页面菜单 + sys_permission 权限点）
- `-- mse-target: platform`（目标=平台控制面库：mse-platform 主库 sys_tenant 系控制面表，仅平台侧脚本使用）
- 逐租户执行的脚本另加 `-- mse-per-tenant: true`

文件名 `V{yyyyMMdd}_{NN}_<biz>__<描述>.sql`（biz 段路由分册账本）。账本 `mse_<biz>_schema_history` 按**文件名**去重（同名表分库各持一份、行集互不重叠）。执行全自动：master 脚本由 `mse.tenant.schema.auto-migrate: true`（默认已开）随服务启动应用主库（失败阻断启动），租户独立库经「租户数据源」界面初始化/懒初始化克隆+迁移自动就绪；base 脚本由 starter-bridge 推送器就绪即推 + 周期重推自愈（`mse.bridge.registry.interval-minutes` 默认 5，总开关 `mse.bridge.registry.enabled` 默认开），经契约 `RemoteBaseDataRegistry` 推给 mse-system 按账本应用（classpath 含 service-system 的应用本地直接应用）。方言目录固定 `mysql` / `dm` / `kingbase`（注意 dm 不是 dameng；现有业务积木仅随附 `mysql/` 脚本，信创方言切换前需自行转译）。方言差异速查表：`references/micro-checklist.md`；号段：`references/alloc-tables.md` §3。

## 命名与版本纪律（硬性）

1. 文件名 `V{yyyyMMdd}_{NN}_<biz>__<描述>.sql`——biz 段路由分册账本 `mse_<biz>_schema_history`，描述段必须含模块名，保证跨积木文件名唯一；同日多件递增 `_02`、`_03`（示例：`V20261001_02_oa__oa_init.sql`、`V20261001_01_oa__oa_base.sql`）。
2. **已应用的脚本不得再修改**（改动即 checksum 冲突，服务启动/推送应用失败）——任何表结构/种子变更一律**新增版本文件**（下一个 `V{yyyyMMdd}_{NN}` 号）。
3. 脚本必须**幂等**（NOT EXISTS / IF NOT EXISTS / 条件 UPDATE）——base 脚本会被周期性重复推送，master 脚本在多实例/重试场景可能重复应用，幂等保证重复执行无害。
4. 管理端「租户数据源」保留主库迁移/结构同步按钮作人工兜底；正常流程无需任何手工执行命令。

## 结构模板（每方言一个版本文件，先 MySQL 后翻译）

1. **头部注释**：工程名/方言/目标库/内容清单/幂等与版本化说明/执行方式（自动：auto-migrate 或 base 推送）；**头指令**：`-- mse-target: master|base|platform`（缺省 master）、逐租户脚本加 `-- mse-per-tenant: true`。
2. **库定位：不写**——脚本目标库由 mse-target 头指令决定（master 缺省主库=缺省共享租户库 mse_tenant），脚本内不写 `USE` / `SET SCHEMA` 等定位语句。
3. **业务表**（mse-target: master）：基列固定——`id BIGINT PK`、`tenant_id BIGINT NOT NULL DEFAULT 0`、`create_by/create_time/update_by/update_time/deleted`（对应 BaseEntity）；业务列居中；`(tenant_id, ...)` 前缀索引；`CREATE TABLE IF NOT EXISTS`（或方言等价判重）。
4. **基座接入**（mse-target: base）：`sys_micro_app` / `sys_menu` / `sys_permission` 三段全 `NOT EXISTS` 判重；权限点 `INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time) VALUES (...)`，**挂本工程页面菜单**（menu_id → sys_menu type=3）；`menu_id=NULL` 仅限登录即授予的通用权限；id 从工程号段基点递增，**写入前先查 alloc-tables 与目标库 `SELECT MAX(id) FROM sys_permission` 确认无冲突**（号段登记处 alloc-tables.md 不变，语义改为「mse-target: base 版本化脚本使用」）。达梦一条一 INSERT 分号结尾 + COMMIT；MySQL/金仓可多值。
5. **独立库租户段：不再写入脚本**——mse_tenant 独立库由「租户数据源」界面初始化/懒初始化克隆 + 迁移自动就绪（demo 的 mse_tenant2 建库建表段已从脚本剔除）；undo_log 等随库对象由迁移通道带过去，无需 SQL 段。
6. **undo_log（仅启用 Seata 时必须存在于各写入库）**：MySQL LONGBLOB/DATETIME(6)、达梦 BLOB/TIMESTAMP(6)、金仓 BYTEA，`UNIQUE (xid, branch_id)`；放在 init 版本化脚本中随主库自动应用。

## 方言差异速查

| 项 | MySQL | 达梦 | 金仓 |
| --- | --- | --- | --- |
| 时间列 | DATETIME + ON UPDATE CURRENT_TIMESTAMP | TIMESTAMP（无 ON UPDATE） | TIMESTAMP（无 ON UPDATE） |
| 小整数 | TINYINT | TINYINT | SMALLINT |
| 表/列注释 | 内联 COMMENT | COMMENT ON | COMMENT ON |
| 索引 | 表内 KEY | 表外 CREATE INDEX | CREATE INDEX IF NOT EXISTS |
| 当前时间 | NOW() | CURRENT_TIMESTAMP | CURRENT_TIMESTAMP |

## 红线

- **不改动框架区 service-system/service-platform 自带的 db/<方言>/ 基线脚本**——业务 SQL 只进业务工程 `<biz>-service` 的 `db/<方言>/` 资源目录，不生成工程级 `sql/` 目录。
- 已应用脚本不再修改（checksum）；变更新增版本文件；脚本保持幂等。
- 表名 `<biz>_` 前缀；权限码与 Controller/@SaCheckPermission、前端 v-permission 一致。
- 大表加必要索引；避免保留字列名（达梦/金仓对保留字更严格）。

## 验证

三方言语法各自校验（至少 MySQL 实跑）：启动服务确认 auto-migrate 应用成功（分册账本 `mse_<biz>_schema_history` 出现该文件名）、CRUD + 权限行为正确；base 脚本条目在 mse-system 账本可查、基座菜单/权限点生效；重复启动/重复推送不产生重复数据。
