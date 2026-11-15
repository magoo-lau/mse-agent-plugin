---
name: review-conventions
description: >
  按 mse 框架规范审查业务工程/代码改动：SPI 自动装配登记、启动类纯净性、事务双注解、
  包名与启动器包规范、端口/权限号段冲突、是否误改框架区、controller 开关语义、前端紧凑风格
  与基座注册。当用户要检查规范合规、code review 业务代码、提交前自查、排查"接口 404/权限
  不生效"类装配问题时使用。
---

# review-conventions —— 业务工程规范审查

依据：`references/conventions.md`（全文）+ `references/base-api.md`（基座消费方式）。审查范围默认 mse-modules 各业务工程与改动 diff；**基座（mse-framework）任何改动都算违规**（mse-gateway.yaml 路由留档与 mse-ui `builtinMicroApps` 兜底登记两类登记式配置数据除外）。

## 后端检查单（按优先级）

1. **底座只读**：`git status/diff` 确认无 mse-framework 文件改动（除 mse-gateway.yaml 路由留档、`builtinMicroApps` 兜底登记两类登记式配置数据）。
2. **SPI 装配**：每个 service 积木有 `META-INF/spring/...AutoConfiguration.imports`；登记的两个全类名与实际类一致；`<Biz>ServiceAutoConfiguration` 带 `@AutoConfiguration(before = MseMybatisAutoConfiguration.class)` + `@MapperScan(..., annotationClass = Mapper.class)`；controller 装配类带 SERVLET 条件 + `mse.service.<biz>.controller` 开关（matchIfMissing=true）。
3. **启动器纯净**：`<biz>-boot` 启动类在 `vip.mseapp.boot.<biz>`、纯 @SpringBootApplication（无 scanBasePackages/@MapperScan/额外注解）；boot 不依赖 service-system/tenant/auth（应引 micro-starter，含 starter-bridge）。
4. **事务双注解**：写方法有 `@Transactional(rollbackFor = Exception.class)`；跨库方法**无** @Transactional 且有 @GlobalTransactional；@GlobalTransactional 类经 @Service 注册而非 @Bean 工厂方法。
5. **命名与归属**：业务包 `vip.mseapp.<biz>`；表 `<biz>_` 前缀；权限码 `<biz>:资源:动作`；无业务/演示代码混入框架区；用不到的能力未被"裁剪"。
6. **RPC**：跨服务仅依赖契约 jar（api-* / xxx-api）；实现带 @RpcService + @Bean @ConditionalOnMissingBean 注册；DTO Serializable；消费基座数据仅经 api-system/api-tenant/api-fs 契约 + starter-bridge（写法见 references/base-api.md），boot 不依赖基座 service-*。
7. **资源分配**：端口/权限号段/网关前缀对照 alloc-tables.md 无冲突且已登记。
8. **SQL**：三方言齐全且语法对应（TIMESTAMP/SMALLINT/COMMENT ON 差异）；权限点 id 在本工程号段内。

## 前端检查单

- 紧凑风格：main.ts 全局 `size: 'small'`，无逐组件重复 size；页面信息密度合理。
- 主题适配：页面样式使用 Element Plus 变量（背景/边框/文字），无硬编码浅色值；主题桥接（theme/bridge.ts + main.ts 暗色基线）未被删除。
- v-permission 权限码与后端一致；401 走 `sub:auth-expired`（不自建登录页）。
- package.json name/base、vite port（唯一）、router fallback、页面菜单（基座「菜单管理」/业务 mse-target: base 版本化脚本登记）与 router 一致。
- `npm run build` 通过。

## 输出格式

按 severity 分组（blocker=违反红线 / major=规范偏离 / minor=建议），每条给 `文件:行号` 与修复建议；引用规范条目（conventions.md §n）。后端改动必须实跑 `mvn install -DskipTests` 作为最终 gate。

## 装配类问题速诊

| 症状 | 首查 |
| --- | --- |
| Controller 404 | imports 未登记 / jar 未进依赖树 / controller 开关被关 |
| Mapper 注入失败 | @MapperScan 包名/annotationClass |
| 权限 403 | sys_permission 无记录 / 权限码不一致 / 登录类型不符 |
| 微服务下鉴权异常 | boot 缺 micro-starter（starter-bridge）或 Nacos 未注册 |
