---
name: micro-gateway-route
description: >
  为 mse 微服务业务在网关配置 mse-gateway.yaml 登记路由（Path=/xxx/** → lb://mse-xxx）
  与免鉴权白名单 mse.gateway.ignore-urls。当用户要加网关路由、配置服务转发、放开回调/公开
  接口的网关鉴权、微服务前端 404 排查时使用。
---

# micro-gateway-route —— 登记网关路由

配置文件：`mse-framework/docker/nacos/config/mse-gateway.yaml`（基座部署配置留档；属**登记式配置数据**，为允许的基座改动之一，见 `references/conventions.md` §2；运行期经 Nacos 热更新）。

## 步骤

1. **查重**：读现文件，确认目标前缀未被占用（已用：/auth /system /tenant /tenant-package /demo /oa /mp /open /openapp /data /openapi/data /ai /openapi/ai /fs /search /monitor；完整表见 `references/alloc-tables.md` §6）。
2. **加路由**（routes 列表，业务服务追加在注释 `# 业务工程与示例` 之下）：

```yaml
- id: mse-<biz>
  uri: lb://mse-<biz>
  predicates:
    - Path=/<前缀>/**
```

多前缀逗号并列（参考 mse-data：`Path=/data/**,/openapi/data/**`）。

3. **白名单（按需）**：仅当接口自带等价或更强鉴权时才放行（登录端点、应用凭证链路、回调验签）。⚠️ `mse.gateway.ignore-urls` 是**整体替换语义**：覆盖时必须全量列出含基座默认项（/auth/login、/auth/platform/login、/auth/tenant/resolve、/auth/domain/config、/actuator/**、/error 及既有业务项）。

```yaml
mse:
  gateway:
    ignore-urls:
      # ...全部默认项原样保留...
      - /<biz>/callback/**   # 示例：业务回调（下游须自行验签）
```

4. **生效**：把改动同步到 Nacos（控制台 http://localhost:18080 或重跑 init-configs.sh），网关运行期热更新无需重启。
5. **验证**：经网关访问业务接口 `http://localhost:48080/<前缀>/...`（带 token），确认 200 与鉴权行为；未带 token 应 401（白名单路径除外）。
6. **登记**：新前缀/白名单写入 `references/alloc-tables.md` §6。

## 排查提示

- 网关 404：服务未注册 Nacos（查 mse-\<biz\> 是否在服务列表）或路由未热更新。
- 401 循环：白名单忘了整体列出被覆盖，或路径大小写不符。
