---
name: gen-ui-subapp
description: >
  按 mse 框架规范创建 wujie 微前端子应用工程（xxx-ui：mse-ui-sub 模板 + v-permission 权限指令
  + hasPermission getter 增强）并完成基座注册（sys_micro_app 或 builtinMicroApps）。当用户要
  新建前端子应用、新建 xxx-ui、接入 mse-ui 基座、微前端接新页面工程时使用。
---

# gen-ui-subapp —— 创建子前端工程

模板：`<插件根>/templates/ui/`（= mse-ui-sub 骨架 + demo-ui 权限增强，已内置紧凑风格/v-permission/401 登出协议）；清单：`references/ui-checklist.md`。

## 第 1 步：收集参数（AskUserQuestion）

| 参数 | 说明 | 默认 |
| --- | --- | --- |
| biz（必填） | 子应用标识（同后端业务名） | —— |
| uiTitle | 显示名（基座菜单/页面标题） | —— |
| uiPort | dev 端口 | 查 alloc-tables 取 4020+ |
| proxyTarget | dev proxy /api 目标 | 单体 http://localhost:8080 / 微服务 http://localhost:48080 |
| 目标目录 | 工程位置 | `<后端工程>/\<biz\>-ui` 或用户指定 |

## 第 2 步：渲染

```bash
node <插件根>/scripts/render.mjs <插件根>/templates/ui <目标目录> \
  --biz=<biz> --ui-port=<port> --ui-title="<显示名>" --proxy-target=<目标>
```

注意：模板中 Vue 插值写成 `{{ x }}`（带空格），占位符 `{{biz}}`（不带空格）——自建页面保持该书写习惯，避免与渲染替换冲突。

## 第 3 步：基座注册（业务 SQL 优先）

- **方式 A（推荐）**：业务工程 jar 内 `db/<方言>/V…_<biz>__base.sql` 版本化脚本（头指令 -- mse-target: base，幂等，**目标=租户库车队自持（共享主库+各独立租户库）**，经 starter-bridge 自动推送 mse-system 按各库账本重放，免手工执行）登记 `sys_micro_app` 注册 + `sys_menu` type=3 页面菜单 + `sys_permission` 权限点；id 号段（子应用注册 id / 菜单号段）先查 `references/alloc-tables.md` §4/§5 选定并登记；配合 micro-create-project 生成的工程已自带该脚本（`--app-id/--menu1..3` 参数渲染）。租户端页面菜单应用后需在「套餐管理」勾选才对该租户可见。
- **方式 B（后台配置）**：平台端「平台管理 → 子应用管理」新增 sys_micro_app 记录（应用地址），字段见 ui-checklist（标识/显示名/devEntry `//localhost:<port>`/prodEntry `/sub-apps/<biz>/`/可见端）；页面级菜单再在「平台管理 → 菜单管理」新增类型=子应用页面的菜单（所属子应用选本应用，路由地址填子应用内部路由）。多租户在「套餐管理」勾选页面菜单（勾选即对该租户启用该子应用）。
- **方式 C（本地开发兜底）**：mse-ui 的 `builtinMicroApps` 兜底登记（登记式配置数据，见 `references/conventions.md` §2）追加同构配置（name/title/devEntry/prodEntry/side，无 menu 字段——页面菜单统一走菜单管理）。

## 第 4 步：验证

```bash
cd <目标目录> && npm install && npm run build   # 构建必须通过
npm run dev                                     # 独立运行 http://localhost:<port>
```

- 基座（mse-ui）登录后侧边栏出现子应用菜单（mse-target: base 脚本或菜单管理登记的子应用页面，未登记则渲染单项；租户端需套餐勾选）；点击经 `sub:navigate` 路由进子应用；
- 登录态：状态条显示用户名；重新登录经 `auth:change` 同步；token 过期 401 → 基座统一登出；
- 按钮权限：`v-permission="'<biz>:xxx:yyy'"` 的按钮对无权限账号不可见。

业务页面接 gen-ui-page；后端接口接 gen-crud。
