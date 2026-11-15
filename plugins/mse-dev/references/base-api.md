# 基座 API 使用指南（base-api）

> 使用方视角：业务工程如何消费 mse 框架基座（mse-framework）提供的能力。系统约定（命名/SPI/事务/红线）见 `conventions.md`，本文只讲"基座给了什么、怎么用"。所有类名均可在基座发布件（`mvn install` 后的本地/私服仓库）中找到。

## 1. 能力总览

| 能力 | 载体 | 业务侧怎么获得 |
| --- | --- | --- |
| 鉴权（登录态 + 按钮权限） | Sa-Token 双账号体系 + `starter-bridge`（随 micro-starter 内置） | boot 引 jar 即得，`@SaCheckPermission` 直接可用 |
| 租户×服务路由（独立库/分表/字段隔离） | bridge 的 `RpcTenantRouteResolver` | `@TenantDatasource` 实体自动切库；SHARED 模式按策略改写 |
| 操作日志 | bridge 的 `RpcOperLogRecorder` | 同上，`@OperLog` 自动落库 |
| 用户/组织/角色/部门数据 | api-system RPC 契约 | `@RpcReference` 注入 |
| 租户/套餐/域名 | api-tenant RPC 契约 | `@RpcReference` 注入 |
| 登录/登出/当前身份 | 基座 HTTP `/auth/**` | 前端经网关直调 |
| 系统管理/租户管理 | 基座 HTTP `/system/**`、`/tenant/**` | 前端直调或平台端后台配置 |
| 子应用登录态下发 | wujie props + bus 事件 | 子应用模板已内置 |

## 2. RPC 契约总览（api-system / api-tenant / api-fs）

业务工程消费基座数据的**唯一合法依赖**是契约 jar（接口 + Serializable DTO）：`vip.mseapp:api-system`、`vip.mseapp:api-tenant`、`vip.mseapp:api-fs`（文件存储，接入写法见 `references/fs-integration.md`）。微服务形态经 Dubbo 远程调用，单体形态（聚合基座 service-* 时）本地直调，业务代码零改动。

### api-system（包 `vip.mseapp.api.system`）

| 契约 | 用途 | 方法 |
| --- | --- | --- |
| `RemoteUserService` | 用户查询与管理 | `getUserByUsername(tenantId, username)`（登录链路，含密码散列）、`getUserById(userId)`、`createTenantAdmin(tenantId, username, passwordHash, nickname)`、`pageUsers(pageNum, pageSize, username, status, deptId)`、`createUser(user)`（superAdmin 强制 FALSE）、`updateUser(user)`（仅 id/nickname/mobile/email/status/deptId 参与更新）、`resetPassword(userId, passwordHash)`、`deleteUser(userId)`、`getUserRoleIds(userId)`、`assignRoles(userId, roleIds)`（覆盖式，空集合=清空） |
| `RemoteAccessService` | 鉴权数据源（bridge 的 Sa-Token `StpInterface` 数据来源） | `getPermissionList(loginType, loginId)`、`getRoleList(loginType, loginId)` |
| `RemoteOrgService` | 组织架构只读查询（审批人、人员/部门/角色选择器） | `getUser(userId)`、`listUsersByIds(userIds)`、`listUsersByDept(deptId, includeChildren)`、`listUsersByRole(roleId)`、`listAllDepts()`、`pageUsers(pageNum, pageSize, keyword, deptId)`、`listRoles()`、`getTenantAdminUserId()`（null=未设置） |
| `RemoteDeptService` | 部门树管理 | `getDeptTree()`、`createDept(parentId, name, sort)`、`updateDept(id, parentId, name, sort, status)`、`deleteDept(id)`（有子部门/在职用户不可删） |
| `RemoteRoleService` | 角色查询与创建 | `listRoles()`、`createRole(role)`（不绑定菜单/权限点，dataScope 缺省仅本人） |
| `RemoteLogService` | 日志落库 | `recordLoginLog(log)`（成功/失败均记录）、`recordOperLog(log)`（`@OperLog` 切面的 RPC 通道） |
| `RemoteConfigService` | 参数配置按键读取（无库微服务读 sys_config，如登录策略开关） | `getConfigValues(tenantId, keys)` |
| `RemoteMessageService` | 站内消息（业务投递 + 双端工作台读取） | `send(receiverIds, type, title, content, bizId)`、`pageMessages(pageNum, pageSize, type, unreadOnly)`、`unreadCount()`、`markRead(id)`、`markAllRead()` |
| `RemoteDataScopeService` | 数据范围解析（个人数据过滤，供不依赖 service-system 的微服务） | `resolve(userId)`（null=不过滤） |

### api-tenant（包 `vip.mseapp.api.tenant`）

| 契约 | 用途 | 方法 |
| --- | --- | --- |
| `RemoteTenantService` | 租户/套餐/域名/隔离路由 | `getTenantById(tenantId)`、`getTenantByCode(code)`、`getTenantByDomain(domain)`（null=未绑定）、`getDomainConfig(domain)`（登录页个性化，未配置返回 null）、`validateTenant(tenantId)`（校验存在/状态/有效期，失败抛 `ServiceException`）、`getPackageMenuIds(tenantId)`、`getPackageMicroAppIds(tenantId)`、`listTenants()`、`getTenantRoute(tenantId, serviceName)`（租户×服务隔离路由，缺省 SHARED_FIELD）、`listTenantDatasources(serviceName)`、`getTenantDatasource(dsKey)`（密码 AES 密文，密钥 `mse.tenant.dynamic-ds.encrypt-key`） |

api-tenant DTO：`TenantDTO`、`DomainConfigDTO`、`TenantDatasourceDTO`、`TenantRouteDTO`、`TablePolicyDTO`（后三者为租户独立库平台化自助管理的数据契约，平台端「租户数据源/隔离策略」界面维护）。

**语义要点**：
- `loginType` 取 `platform` / `tenant`；`loginId` 平台端=用户编号，租户端=`"{tenantId}-{userId}"`。
- `getPackageMenuIds`：null=未配套餐（不裁剪）；空列表=套餐未勾选任何菜单。
- 子应用可用性由 `getPackageMenuIds` 推导：租户菜单集含该应用任一 type=3 页面即可用；无页面菜单的应用对所有租户可用。
- 密码一律传 BCrypt 散列，由调用方完成散列。

## 3. 消费方式（@RpcReference）

`<biz>-boot` 的依赖（模板已生成）：

```xml
<dependency>vip.mseapp:micro-starter</dependency>     <!-- 微服务形态入口：Nacos+Dubbo+starter-bridge 桥接+Seata+驱动 -->
<dependency>vip.mseapp:api-system</dependency>          <!-- 按需 -->
<dependency>vip.mseapp:api-tenant</dependency>          <!-- 按需 -->
```

注入与调用：

```java
@RpcReference
private RemoteOrgService remoteOrgService;

// 无需携带 tenantId——租户/用户上下文经 RPC 隐式透传（Dubbo attachment），
// provider 端自动恢复，契约方法在调用方租户上下文内执行。
List<OrgDeptDTO> depts = remoteOrgService.listAllDepts();
```

- 注解全名 `vip.mseapp.core.annotation.RpcReference`（属性 `version`/`group`/`check`（默认 false）/`timeout`（默认 5000ms））。
- **双形态零改动**：`mse.rpc.mode=dubbo` 远程调用 mse-system/mse-platform；`local`（单体）从容器解析基座本地 Bean 直调。
- 实际用例：`mse-modules/mse-oa`（`RpcOaOrgService` 把基座组织数据适配为工作流审批人数据源）、`mse-modules/mse-open`（`OpenUserSyncService` 编排用户+部门+角色契约）。
- 业务自身对外契约的定义/提供见 gen-rpc-contract 技能；本文只讲消费基座。

## 4. starter-bridge 自动提供的能力

业务 boot 引入 micro-starter（内置 `starter-bridge`）即自动获得（Spring Boot SPI 装配，均带让位语义：单体聚合了基座 service-* 时基座本地实现优先）：

| 自动 Bean | 实现的接口 | 效果 |
| --- | --- | --- |
| `RpcSaPermission` | Sa-Token `StpInterface` | 业务服务里 `@SaCheckPermission` / `@SaCheckRole` 直接可用；权限/角色数据经 RPC 向基座拉取，按"版本号+TTL"缓存 |
| `RpcTenantRouteResolver` | `vip.mseapp.core.datasource.TenantRouteResolver` | 按 (租户, 服务) 解析路由快照（30s 本地缓存，解析失败退回共享库缺省） |
| `RpcTenantDsMetadataProvider` | `vip.mseapp.mybatis.datasource.TenantDsMetadataProvider` | 租户数据源元数据 RPC 通道（密文传输，各服务共享 encrypt-key 解密） |
| `RpcOperLogRecorder` | `vip.mseapp.web.log.OperLogRecorder` | `@OperLog` 注解的操作日志自动经 RPC 落基座 `sys_oper_log` |

**注意**：bridge 不含组织/用户查询的回退实现——业务需要组织/用户数据时，自行 `@RpcReference RemoteOrgService` / `RemoteUserService`（参考 mse-oa 的 `RpcOaOrgService`）。

## 5. 登录态与上下文（业务服务内取当前用户/租户）

| 工具类 | 常用方法 | 说明 |
| --- | --- | --- |
| `vip.mseapp.core.context.TenantContext` | `getTenantId()`、`getRequiredTenantId()`（缺失快速失败）、`isPlatform()`、`runAs(tenantId, action)`、`runIgnore(action)`、`clear()` | `runAs` 指定租户执行；`runIgnore` 跳过隔离（平台管理/定时任务） |
| `vip.mseapp.core.context.UserContext` | `get()`、`getUserId()`、`getUsername()`、`isPlatform()`、`clear()` | 认证后由 common-security 写入 |
| `vip.mseapp.security.stp.StpTenantUtil` | `login()`/`logout()`/`isLogin()`/`getUserId()`/`getTenantIdFromLoginId()` | 租户端账号体系（loginId=`{tenantId}-{userId}`） |
| `vip.mseapp.security.stp.StpPlatformUtil` | 同上 | 平台运营端账号体系（loginId=userId） |

- 租户请求头：`X-Tenant-Id`（`MseConstants.HEADER_TENANT_ID`）；认证拦截器会用 **token 内租户覆盖请求头**，防止伪造。
- 平台租户固定编号：`MseConstants.PLATFORM_TENANT_ID = 0L`。
- 控制器鉴权注解：`@SaCheckPermission(value = "<biz>:<资源>:<动作>", type = MseConstants.LOGIN_TYPE_TENANT)`。

## 6. 基座 HTTP API（经网关，默认 48080）

前端或开放场景直调，统一携带 `Authorization` 请求头。常用端点：

| 分组 | 端点 | 用途 |
| --- | --- | --- |
| 认证 `/auth` | `POST /auth/login` | 租户端登录（tenantId/tenantCode + 账密），返回 `{tokenName, tokenValue, tenantId, userId, nickname, platform}` |
| | `POST /auth/platform/login` | 平台运营端登录 |
| | `GET /auth/tenant/resolve?host=` | 登录页按主机名定位租户（免鉴权） |
| | `GET /auth/domain/config?host=` | 登录页个性化配置（免鉴权） |
| | `GET /auth/me` · `POST /auth/logout` | 当前登录身份 / 登出 |
| 系统 `/system` | `/system/user`、`/system/role`、`/system/dept`、`/system/menu`、`/system/permission` | 用户/角色/部门/菜单/权限点管理（分页 CRUD，`/platform/*` 为平台端账号专用） |
| | `GET /system/profile/me` | 当前用户信息 |
| | `/system/micro-app`（`GET /list`、`POST`、`PUT`、`POST /{id}/status`、`DELETE /{id}`） | **子应用注册管理（第三方前端接入入口）** |
| | `/system/config`、`/system/dict/**`、`/system/post` | 参数/字典/岗位 |
| 租户 `/tenant` | `/tenant`（page/get/post/put/status） | 租户开通与管理 |
| | `/tenant-package` | 套餐（圈定菜单；推导子应用可用性） |
| | `/tenant/domain-config` | 租户域名绑定与登录页定制 |

网关免鉴权白名单默认项：`/auth/login`、`/auth/platform/login`、`/auth/tenant/resolve`、`/auth/domain/config`、`/actuator/**`、`/error`。业务新增免鉴权接口经 micro-gateway-route 技能登记（`mse.gateway.ignore-urls` 整体替换语义）。

## 7. 前端子应用协议（wujie 微前端）

基座经 wujie props 下发：`{ urlPrefix, token, tenantId, platform, nickname, permissions, theme }`（`permissions` 超管为 `['*']`；`urlPrefix` 为子应用路由前缀 `/sub/<name>`）。

bus 事件（子应用经 `window.$wujie.bus` 收发）：

| 事件 | 方向 | payload | 用途 |
| --- | --- | --- | --- |
| `auth:change` | 主→子 | `{token, tenantId, platform, nickname, permissions}` | 登录态变化广播（alive 模式下 props 不更新，靠它刷新） |
| `sub:auth-expired` | 子→主 | — | 子应用收到 401 时上报，基座统一登出（子应用不自建登录页） |
| `sub:navigate` | 主→子 | `{urlPrefix, path}` | 基座菜单驱动子应用路由跳转 |
| `sub:ready` | 子→主 | `{urlPrefix}` | 子应用挂载完成上报，主应用补发当前目标路由 |
| `sub:title` | 子→主 | `{urlPrefix, path, title}` | 路由切换后上报当前页标题（路由 `meta.title`），主应用用于页签与面包屑中文展示 |

子应用模板（`templates/ui/`）已内置以上协议、`v-permission` 按钮权限指令与 auth store `hasPermission` getter；子应用创建与注册见 gen-ui-subapp 技能。

## 8. 权限点与菜单接入

1. 权限点 SQL 放业务工程 jar 内 `db/<方言>/V…__<biz>_base.sql`（头指令 -- mse-target: base；**目标=租户库车队自持副本**，写 `sys_permission`；该表随 base 脚本自持于共享主库与各独立租户库；starter-bridge 自动推送 mse-system 车队重放到租户库（各库自持副本）按账本应用，免手工执行）：`menu_id=NULL` 即通用权限（登录即授予）；挂到菜单才参与套餐裁剪——gen-sql 技能生成。
2. 页面菜单与权限点：版本化脚本（头指令 `-- mse-target: base`）幂等登记 `sys_menu` type=3 子应用页面（`micro_name` 关联子应用标识），或平台端「菜单管理/权限点管理」配置；页面菜单随菜单树参与套餐裁剪（页面级粒度）。
3. 子应用注册：版本化脚本（头指令 `-- mse-target: base`）登记 `sys_micro_app`（优先），或平台端「子应用管理」配置；本地开发/零配置兜底登记 mse-ui 的 `builtinMicroApps`（登记式配置数据，见 conventions.md §2）。
4. 多租户可见性：由菜单推导——**租户端页面菜单需在套餐 menu_ids 勾选才可见**，勾选即对该租户启用该子应用；未登记页面菜单的应用对所有租户可用。
