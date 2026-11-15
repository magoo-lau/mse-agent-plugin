# mse-dev —— ai-mse 业务开发技能包

按 mse 框架规范（`references/conventions.md` + `references/base-api.md`）辅助第三方业务开发：创建业务工程（微服务/单体两类，skills 分目录）、生成规范代码（CRUD / RPC 契约 / 三方言 SQL / 子应用页面等）、消费基座能力。安装方式见市场根 README。

## 使用约定

- 所有 skill 的触发都围绕 mse 脚手架：使用前确认本机已具备 ai-mse 仓库，且框架区已 `mvn install -DskipTests`（业务工程编译依赖底座发布件）。
- 模板定位：**插件根 = 本 SKILL.md 所在目录的上两级**（skills/<分类>/<skill>/SKILL.md → 插件根），`templates/`、`scripts/`、`references/` 均在插件根下。
- 规范红线（所有 skill 共享，详见 `references/conventions.md`）：
  1. 业务/示例代码禁止写进 mse-framework；新业务工程放 `mse-modules/<工程名>/`，独立 Maven 工程，不进根 pom。
  2. 用不到的能力不裁剪底座——不引 jar、不登记子应用即可，全程不修改框架区代码（网关路由留档 yaml 与 mse-ui `builtinMicroApps` 兜底登记两类登记式配置数据除外）。
  3. 服务积木必须经 Spring Boot SPI 接入；启动器纯 `@SpringBootApplication`，启动类放 `vip.mseapp.boot.<biz>`。
  4. 写操作 `@Transactional(rollbackFor = Exception.class)`；跨库全局入口只用 `@GlobalTransactional`（禁叠加 `@Transactional`）。
  5. 消费基座能力只经 `api-system`/`api-tenant` 契约 + micro-starter 内置的 `starter-bridge`，不依赖基座 service-*（契约清单与写法见 `references/base-api.md`）。
  6. 端口与权限号段先查 `references/alloc-tables.md`，避免冲突。
