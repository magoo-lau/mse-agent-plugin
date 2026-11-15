---
name: micro-nacos-config
description: >
  管理 mse 微服务业务的服务专属 Nacos 配置：docker/nacos/config/mse-xxx.yaml 本地留档 +
  init-configs.sh 导入 Nacos 配置中心。当用户要改服务端口、配置独立库租户数据源、启停
  RocketMQ/Seata、切换达梦/金仓 profile、或启动报配置缺失时使用。
---

# micro-nacos-config —— 服务专属 Nacos 配置

留档位置：`<工程>/docker/nacos/config/mse-<biz>.yaml`（Data ID: `mse-<biz>.yaml`，Group: DEFAULT_GROUP）。导入脚本：`mse-framework/docker/nacos/init-configs.sh`。

## 配置优先级（排查配置问题先看这个）

服务专属 Data ID（mse-\<biz\>.yaml）> 共享配置（mse-micro-common.yaml / mse-micro-db-biz.yaml / mse-micro-dubbo.yaml，`spring.config.import` 列表内靠后者优先）> 应用内配置。**共享项不写在专属配置里**：master 数据源/Redis/Sa-Token/MyBatis-Plus/Dubbo/租户开关在 mse-micro-common.yaml / mse-micro-db-biz.yaml（业务服务用 -biz 版；基座服务与 mse-monitor 用 mse-micro-db.yaml，master=平台库）。

## 典型任务

1. **改端口**：改留档 yaml `server.port`（与 Dockerfile/k8s 的 ARG PORT、alloc-tables 登记一致）→ 导入 → 重启服务。
2. **开独立库租户数据源**：优先平台端「平台管理 → 租户数据源」界面维护并绑定租户（运行时动态注册、无需改配置/重启；达梦/金仓初始化手动）；仅兼容场景才取消留档 yaml 中 tenant2 静态段注释（同 key 时界面维护的动态数据源优先）→ 导入。
3. **启用 RocketMQ**：pom 放开 common-mq（service+boot 两处）→ 留档 yaml 放开 rocketmq 段 → 导入。
4. **启用 Seata**：无需改 pom（Seata 已随 micro-starter 内置）→ 确认 MSE_SEATA_ENABLED=true（共享配置）→ 删除留档 yaml 中 `spring.datasource.dynamic.seata: false` → 业务 SQL 含 undo_log → 跨库方法只用 @GlobalTransactional。
5. **信创切换**：启动参数 `--spring.profiles.active=dm|kingbase`（master 数据源/方言由共享 mse-micro-db-biz.yaml profile 段提供，业务基线无需配置）。

## 导入命令

```bash
CONFIG_DIR=<仓库根>/<工程路径>/docker/nacos/config sh mse-framework/docker/nacos/init-configs.sh
```

## 验证

- Nacos 控制台（http://localhost:18080）确认 Data ID 内容已更新；
- 服务重启后 actuator/health 正常（开启令牌的集群带头验证），业务表读写正常；
- 运行期临时调整直接改 Nacos 控制台内容（留档 yaml 同步改，保持一致），refresh:true 的项即时生效。
