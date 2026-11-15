---
name: mono-aggregate-module
description: >
  把已有 mse 业务积木（xxx-service jar）聚合进现有单体工程（monolith）。当用户说
  "把 xx 业务加进单体"、"单体集成 xx 模块"、"monolith 加新业务"时使用；含依赖追加、
  SQL 初始化与验证清单。
---

# mono-aggregate-module —— 单体聚合业务积木

对象：已有单体工程（如 `mse-sample/mse-monolith` 或 mono-create-project 生成的工程）。业务积木本体不动——聚合 = 加一条依赖。

## 步骤

1. **前置检查**：
   - 业务工程已 `mvn install`（单体按坐标从本地/私服仓库解析）；
   - 单体 pom parent=mse-starter-parent（或自定 parent 已 import mse-dependencies BOM）；
   - 目标业务无 boot 依赖要求（聚合的是 `<biz>-service` 积木，不是 `<biz>-boot`）。
2. **追加依赖**（单体 pom dependencies，版本随 BOM/工程）：

```xml
<dependency>
    <groupId>vip.mseapp</groupId>
    <artifactId><biz>-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>   <!-- 未入 BOM 的业务工程需显式版本 -->
</dependency>
```

   oa 特例：引 `oa-service` 聚合件（内含 oa-form + oa-bpm），不引散件。
3. **SQL 初始化（免手工执行）**：业务积木 jar 内 `db/<方言>/` 版本化脚本自动应用——`V…_<biz>__init.sql`（头指令 -- mse-target: master，业务表）随单体启动 auto-migrate 应用主库（`mse.tenant.schema.auto-migrate: true` 已开则生效，失败阻断启动），`V…_<biz>__base.sql`（头指令 -- mse-target: base，子应用注册+页面菜单+权限点）经 starter-bridge 推送 mse-system 车队重放到租户库（共享主库+各独立租户库各持副本，周期重推自愈）。已有数据的环境先核对权限点/菜单 id 号段不冲突（references/alloc-tables.md §3/§5）。
4. **编译启动**：`mvn install -DskipTests` → 启动单体。
5. **验证清单**：
   - [ ] 登录正常（聚合不影响基座功能）；
   - [ ] 新业务接口可访问，@SaCheckPermission 权限点生效（无权限账号应 403）；
   - [ ] 业务内 @RpcReference 契约消费为本地直调（local 模式，bridge 自动让位）；
   - [ ] 控制台无 SPI 装配告警（AutoConfiguration.imports 未生效时接口 404，优先查 jar 是否真的进了依赖树：`mvn dependency:tree | grep <biz>`）。

## 常见问题

| 症状 | 原因 |
| --- | --- |
| 接口 404 | jar 未进依赖树，或业务积木 SPI 文件缺失 |
| Mapper 注入失败 | 积木未按 SPI 标准做 @MapperScan（用 review-conventions 查） |
| 权限 403 | 基座接入脚本（mse-target: base）未应用（sys_permission 无记录，查 mse-system 账本/管理端「API 目录」与权限点）或权限码不一致 |
