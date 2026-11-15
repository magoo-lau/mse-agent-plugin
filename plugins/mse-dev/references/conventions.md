# mse 框架使用规范总览（conventions）

> 本文档是 mse-dev 插件所有 skill 共享的规范依据：系统约定（本文 §1–§9）+ 技能使用约束（§10）+ 基座能力速览（§11）。基座 API 的具体使用方式见 `references/base-api.md`。

## 1. 命名规范（业务侧）

| 形态 | 命名 | 说明 |
| --- | --- | --- |
| 业务模块 | `<biz>-boot`/`<biz>-service`/`<biz>-api`/`<biz>-ui` | 业务名开头；多积木聚合层叫 `<biz>-service`（如 oa-form+oa-bpm → oa-service） |
| 微服务 | 名字含 `boot` 的启动器 | 应用名 `mse-<biz>`，application.yaml 经 spring.config.import 接入 Nacos，网关 `lb://mse-<biz>` |
| 聚合单体 | 名字含 `monolith` | `mse.rpc.mode=local`，默认仅需 MySQL/Redis（MQ 可选：`mq-starter` + rocketmq 配置，与形态无关）；聚合业务 = 引入 `<biz>-service` jar |

- 包名：业务包 `vip.mseapp.<biz>`；启动类 `vip.mseapp.boot.<biz>`（与业务积木包树零重叠）。
- 业务工程放 `mse-modules/<工程名>/`，各自独立 Maven 工程，不进根 pom 聚合。
- 底座模块以 `boot-`/`api-`/`common-`/`service-` 开头，**对使用方只读**：业务代码不得新增底座模块，也不得仿用这些前缀命名自己的模块。

## 2. 积木拼法（最高优先级）

- 框架基座只提供与业务无关的基础能力；**业务/演示代码禁止写进基座（mse-framework）**。
- 用不到的能力不"裁剪"：不引对应 jar、不在注册表登记子应用即可。
- **基座只读红线**：全程不修改 mse-framework 代码；仅允许两类"登记式配置数据"改动——
  1. `mse-framework/docker/nacos/config/mse-gateway.yaml` 路由/白名单留档（运行期经 Nacos 热更新）；
  2. `mse-framework/mse-ui/src/micro/apps.ts` 的 `builtinMicroApps` 本地开发兜底登记（平台端「子应用管理」配置优先）。
- 可替换点：基座各默认实现（Service、RPC 契约实现、鉴权数据源、租户路由等）均带 `@ConditionalOnMissingBean(契约类型)` 回退——声明同契约 Bean 即整体替换；默认控制器可用 `mse.service.<name>.controller.enabled=false` 关闭后由自有控制器替代。
- 新业务工程参考 `mse-sample/mse-demo/`（微服务）与 `mse-sample/mse-monolith/`（单体）。

## 3. SPI 接入标准（service 层硬性要求）

每个服务积木必须经 Spring Boot SPI 接入：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记两个自动装配类：

1. `<Biz>ServiceAutoConfiguration`（放 `autoconfigure` 包）：
   - `@AutoConfiguration(before = MseMybatisAutoConfiguration.class)`
   - `@MapperScan("<package>.<biz>.mapper", annotationClass = Mapper.class)`
   - `@ComponentScan` 非 controller 组件包（service/support/config/engine/runtime/mq/security 等）
   - 可整体替换的契约实现用 `@Bean + @ConditionalOnMissingBean(契约类型)` 显式声明
2. `<Biz>ControllerAutoConfiguration`：
   - `@AutoConfiguration` + `@ConditionalOnWebApplication(SERVLET)`
   - `@ConditionalOnProperty(prefix = "mse.service.<biz>.controller", name = "enabled", matchIfMissing = true)`
   - `@ComponentScan(controller 包)`

**启动器一律纯 `@SpringBootApplication`**：无 `scanBasePackages`、无 `@MapperScan`；启动类统一放 `vip.mseapp.boot.<biz>`（与业务积木包树零重叠），SPI 是唯一装配入口。

`@GlobalTransactional` 的 bean 必须用 `@Service` 等组件扫描注册，不能经 AutoConfiguration `@Bean` 工厂方法注册（Seata 2.6 预扫描会静默跳过工厂方法 bean）。

## 4. 事务双注解规范

- 写操作方法一律 `@Transactional(rollbackFor = Exception.class)`（单体单库事务保障 + 微服务全局分支）。
- 跨服务/跨库全局入口再叠加 `@GlobalTransactional`（Seata 关闭/缺失时被忽略）。
- **例外**：一个方法内跨多库路由（写主库 + 租户库）**禁止 `@Transactional`**（连接绑定先于路由决策），只用 `@GlobalTransactional`（参考 mse-demo `DemoOrderService.placeOrder`）。

## 5. 微服务形态要点

- `<biz>-boot` 依赖就两件：`micro-starter`（微服务形态入口启动器：Nacos 配置/注册 + Dubbo RPC + starter-bridge 契约消费桥接 + Seata 内置 + actuator + 三数据库驱动）+ 自有 `<biz>-service`（+ 按需 `mq-starter`）。
- **不依赖基座 service-system/platform/auth**——登录/系统管理收敛在基座微服务，业务服务经 starter-bridge 以契约消费基座能力（鉴权数据源/租户路由/操作日志由 bridge 的 RPC 回退实现提供，组织/用户查询自行 `@RpcReference`，详见 `references/base-api.md`）。
- 本地仅 `application.yaml`（应用名 + Nacos 地址 + spring.config.import 引共享 `mse-micro-common/db/dubbo.yaml` 与服务专属 Data ID；SCA 2025.1 起 bootstrap 装载已移除）；数据源/Redis/Sa-Token/Dubbo 统一在 Nacos 管理，`mse.rpc.mode` 由 micro-starter 默认 dubbo（共享 `mse-micro-dubbo.yaml` 亦显式提供，显式配置可覆盖）。
- 服务专属配置留档在工程 `docker/nacos/config/<应用名>.yaml`，经 `CONFIG_DIR=<该目录> sh mse-framework/docker/nacos/init-configs.sh` 导入 Nacos。
- 网关路由在 `mse-framework/docker/nacos/config/mse-gateway.yaml` 留档（`Path=/<前缀>/** → lb://mse-<biz>`），按需补 `mse.gateway.ignore-urls`（整体替换语义：覆盖时必须全量列出）。
- 信创库切换：`--spring.profiles.active=dm|kingbase`（数据源/方言由共享 `mse-micro-db.yaml` profile 段提供）。

## 6. 单体形态要点

- 无 boot/Nacos/Dubbo：直接把 `<biz>-service` jar 加进聚合单体 pom 依赖（SPI 自动装配），启动类纯 `@SpringBootApplication`。
- `application.yaml` 关键项：`mse.rpc.mode: local`、`mse.tenant.enabled: true`、dynamic 数据源（master + 可选 tenant2，`lazy: true`、`strict: false`）、mybatis-plus（assign_id + 逻辑删除 deleted）、sa-token。
- 聚合 oa 用 `oa-service` 聚合件而非散件（oa-form/oa-bpm）。

## 7. RPC 契约

- 对外契约放 `<biz>-api`（接口 + Serializable DTO，参考 demo-api）；实现类标 `@RpcService(契约.class)`，由装配类 `@Bean + @ConditionalOnMissingBean` 注册；消费方 `@RpcReference` 注入。
- 可选注解属性（框架镜像注解，按模式生效；**外层无 dubbo 扁平属性，调参一律走 dubbo() 嵌套**）：`@RpcService(dubbo = @RpcService.Dubbo(version=..., timeout=...))` 为 @DubboService 全量属性镜像、`local = @RpcService.Local(...)` 配置 bean 名/primary 消歧；`@RpcReference(dubbo = @RpcReference.Dubbo(timeout=..., retries=...))` 为 @DubboReference 全量属性镜像（缺省 timeout=5000/check=false）、`local = @RpcReference.Local(required=false)` 配置 local 容错注入。方法级配置在 `dubbo().methods` 直用 Dubbo 原生注解：`methods = {@Method(name=..., timeout=...)}`（import org.apache.dubbo.config.annotation.Method；dubbo 经 common-core 编译期传递，无需模块自加依赖）。单体可配 `mse.rpc.local.scan-packages` 启用兜底自动注册（漏写 @Bean 的安全网，@Bean 永远优先）。详见框架 `docs/tech/common-rpc.md`。
- 单体 local 模式本地直调，微服务 dubbo 模式远程调用，消费方代码零改动；租户上下文经 RPC 隐式透传，契约方法无需携带 tenantId。
- 消费基座能力（不是对外提供）：micro-starter 内置 starter-bridge，消费 `api-system`/`api-tenant` 契约；可消费的契约清单与写法见 `references/base-api.md`。
- 基座控制器开关：`mse.service.<system|platform|auth>.controller.enabled=false` 整体关闭后自有控制器替代；业务积木同款开关 `mse.service.<biz>.controller.enabled`。

## 8. 权限点与 SQL（版本化迁移脚本）

- 业务 SQL 为 **jar 内版本化迁移脚本**（不再有工程级 `sql/` 目录），放 `<biz>-service/src/main/resources/db/<方言>/`（单层目录，方言 `mysql/dm/kingbase`，dm 不是 dameng；业务积木当前仅随附 `mysql/` 脚本，信创方言需切换前自行转译）。目标库由脚本头指令声明：`-- mse-target: master|base|platform|tenant`（缺省 master）——master=业务主库（默认=缺省共享租户库 mse_tenant，随租户库克隆进各租户库）、base=租户域共享数据（sys_micro_app 注册 + sys_menu type=3 页面菜单 + sys_permission 权限点等全局表与租户域种子，随车队重放自持于租户库；幂等）、platform=平台库 mse_platform（mse-platform 主库：sys_tenant 系控制面表 + 平台身份表/全局表/平台种子，平台账号 tenant_id=0 只进此库）、tenant=租户库身份表直建（service-system 专用）；两库两份维护详见 docs/tech/platform-tenant-divide.md；逐租户执行的脚本另加 `-- mse-per-tenant: true`。**红线：不改动框架区 service-system/service-platform 自带的 db/<方言>/ 基线脚本**。
- **免手工执行**：master 脚本由 `mse.tenant.schema.auto-migrate: true`（默认已开）随服务启动应用主库（失败阻断启动；多实例 Redis 锁按目标库互斥），租户独立库经「租户数据源」界面初始化/懒初始化克隆+迁移自动就绪；base 脚本由 starter-bridge 就绪即推 + 周期重推自愈（`mse.bridge.registry.interval-minutes` 默认 5，`mse.bridge.registry.enabled` 默认开），经契约 `RemoteBaseDataRegistry` 推给 mse-system 按分册账本应用（单体/基座应用 classpath 含 service-system 时本地直接应用）。
- **版本纪律**：命名 `V{yyyyMMdd}_{NN}_<biz>__<描述>.sql`（biz 段路由分册账本 `mse_<biz>_schema_history`，同名表分库各持一份、按文件名去重；描述段含模块名保证跨积木唯一）；已应用脚本不再修改（checksum 冲突），变更一律新增版本文件；脚本保持幂等（NOT EXISTS / IF NOT EXISTS / 条件 UPDATE），重复推送/应用无害。
- 权限点 INSERT 一律挂本工程页面菜单（`menu_id` 指向 `sys_menu` type=3）才参与授权分组与套餐裁剪；`menu_id=NULL` 仅限登录即授予的真正全局权限（如个人资料）；历史 NULL 数据由 base 版本化脚本文末幂等 UPDATE 补挂。
- 权限码格式 `<biz>:<资源>:<动作>`（如 `demo:order:list`）；控制器 `@SaCheckPermission(value = ..., type = MseConstants.LOGIN_TYPE_TENANT)`。
- 表基列固定：`id BIGINT PK + tenant_id + create_by/create_time/update_by/update_time/deleted`（对应 `BaseEntity`）；实体继承 `vip.mseapp.mybatis.data.BaseEntity`。
- 租户隔离零配置：业务表继承 `BaseEntity` 即由基座租户插件自动行级隔离；确需平台级全局表（无 tenant_id 列）时实体继承 `GlobalBaseEntity`，框架自动豁免（基类即声明，禁止再配 `mse.tenant.ignore-tables`）。平台公共行（tenant_id=0）读取用 `TenantContext.runAsPlatform`，平台跨租户管理操作用 `@TenantIgnore`，无上下文链路（开放 API 鉴权）用 `runIgnore`。
- 服务身份声明：ServiceAutoConfiguration 类标 `@TenantServiceModule("mse-<biz>")`（mapper 包根缺省按所在包推导）——单体聚合接入时「租户隔离策略」服务级配置零 yaml 生效；`mse.tenant.service-mappings` 为逃生舱（优先级更高）。
- 取单行走分页插件按方言翻译（`selectPage(Page.of(1,1,false), ...)`），不用 `LIMIT 1`（信创库兼容）。

## 9. 前端（wujie 微前端）

- 子应用放业务工程 `<biz>-ui`；以 `mse-sample/mse-ui-sub/` 为模板；全局紧凑风格 `size: 'small'`（main.ts 已内置，不逐组件重复写）。外观主题由基座统一管理：模板内置 `theme/bridge.ts` 经 props.theme / bus `theme:change` 跟随基座深浅色与主色，页面样式一律用 Element Plus 变量（不硬编码浅色值）。
- 基座注册三通道：业务工程 jar 内版本化脚本（头指令 `-- mse-target: base`）幂等落库（starter-bridge 自动推送 mse-system 车队重放到租户库（各库自持副本），免手工执行，优先）→ 平台端「子应用管理」+「菜单管理」配置 → 无配置回退 mse-ui 的 `builtinMicroApps` 兜底登记（登记式配置数据，见 §2）。
- 登录态：基座经 props 下发 `{ urlPrefix, token, tenantId, platform, nickname, permissions }`，变化经 bus `auth:change` 广播；401 经 `sub:auth-expired` 交基座登出。
- 按钮权限 `v-permission` 指令 + auth store `hasPermission` getter。
- dev proxy `/api` 指向同一后端：单体 8080 / 网关 48080。
- 子应用协议细节（props/事件/端点）见 `references/base-api.md` §7。

## 10. 技能使用约束与验证底线

- **资源分配**：新业务的端口/权限号段/网关前缀先查 `references/alloc-tables.md`，避免与基座及已登记业务冲突；用后登记回该表。
- **技能路由**：按形态选技能——微服务 micro-*、单体 mono-*、通用 shared-*（完整路由表见 mse-conventions 技能）；mse-conventions 只做问答与分发，不代做具体工作。
- **规范审查**：提交前走 review-conventions 技能；问题引用格式 `conventions.md §n`。
- **验证底线**：后端改动须 `mvn install -DskipTests` 通过（前置：基座已发布到本地/私服仓库）；前端改动须 `npm run build` 通过。
- 文档同步：业务功能文档放业务工程自己的 README。

## 11. 基座系统能力速览

业务工程可消费的基座能力一览（详细签名/写法/端点见 `references/base-api.md`）：

| 能力 | 获得方式 |
| --- | --- |
| 鉴权（`@SaCheckPermission` 可用） | micro-starter 内置 starter-bridge，权限数据经 RPC 向基座拉取并缓存 |
| 租户多模式隔离（`@TenantDatasource`/分表/字段隔离） | 同上（bridge 的 `RpcTenantRouteResolver`） |
| 操作日志（`@OperLog` 落库） | 同上（bridge 的 `RpcOperLogRecorder`） |
| 用户/组织/角色/部门数据 | `@RpcReference` 注入 api-system 契约（RemoteUserService/RemoteOrgService 等） |
| 租户/套餐/域名 | `@RpcReference` 注入 api-tenant 契约（RemoteTenantService） |
| 当前用户/租户上下文 | `UserContext` / `TenantContext`（runAs/runIgnore）；租户经 RPC 隐式透传 |
| 登录/系统管理/租户管理 HTTP API | 经网关 `/auth/**`、`/system/**`、`/tenant/**`（前端直调或平台端配置） |
| 子应用登录态与事件 | wujie props + bus 事件（auth:change / sub:auth-expired / sub:navigate / sub:ready / sub:title——路由切换上报 `meta.title` 供基座页签与面包屑显示中文），模板已内置 |
| 文件存储（mse-fs 优先/本地降级） | 引 `api-fs` 契约 + 双嵌套条件装配（见 `references/fs-integration.md`） |
| 业务能力开放（第三方调用） | 依赖 mse-open 的 `open-api` 契约，声明 `OpenApiContributor` Bean 自注册目录（见 gen-rpc-contract 技能 C 节） |
