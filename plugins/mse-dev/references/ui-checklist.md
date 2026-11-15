# 子应用（<biz>-ui）清单（ui-checklist）

> 蓝本 `mse-sample/mse-ui-sub/`（骨架）+ `mse-demo/demo-ui/`（权限指令增强），模板位于插件根 `templates/ui/`。

## 工程结构（模板渲染产物）

```
<biz>-ui/
├── package.json             【P】name=<biz>-ui；build 脚本 --base=/sub-apps/<biz>/
├── vite.config.ts           【P】server.port=<UI_PORT>（唯一）；proxy /api → {{PROXY_TARGET}}（单体 8080 / 网关 48080）
├── tsconfig.json / index.html 【P】标题=<UI_TITLE>
└── src/
    ├── main.ts              已内置：ElementPlus size:'small'（紧凑风格）+ dark/css-vars 暗色基线 + v-permission 指令注册
    ├── App.vue              状态条（仅独立运行显示，嵌入态不渲染） + sub:navigate/sub:ready/sub:title bus 协议 + 主题桥接初始化（勿删）
    ├── theme/bridge.ts      主题桥接：跟随基座深浅色/主色（props.theme 初始值 + theme:change 事件；独立运行读本地偏好）
    ├── api/request.ts       token 注入 + 401 → sub:auth-expired + 业务错误码透传（勿重复实现）
    ├── directives/permission.ts   v-permission（数据源 = auth store）
    ├── stores/auth.ts       props 初始值 + auth:change 订阅 + hasPermission getter
    ├── router/index.ts      【P】fallback 前缀 /sub/<biz>；业务路由表在此登记
    ├── utils/env.ts / style.css / env.d.ts   原样保留（style.css 禁用 100vh）
    └── views/               业务页面（gen-ui-page 生成；模板仅留 Home.vue）
```

## 基座注册（二选一，后台优先）

**方式 A（推荐）**：业务工程 jar 内 `db/<方言>/V…__<biz>_base.sql` 版本化脚本（头指令 -- mse-target: base）幂等落库（**目标=租户库车队自持**：共享主库+各独立租户库各持 sys_micro_app/sys_menu/sys_permission 副本；starter-bridge 就绪即推 + 周期重推自愈，推给 mse-system 按各库账本重放，免手工执行；配合 micro-create-project 生成，`--app-id/--menu1..3` 定号段）：

| 表 | 内容 |
| --- | --- |
| sys_micro_app | 应用地址：name=`<biz>`（路由前缀 /sub/\<biz\>）/ title / devEntry `//localhost:<UI_PORT>` / prodEntry `/sub-apps/<biz>/`（与 nginx 部署路径一致）/ side tenant=1、platform=2、both=3 |
| sys_menu type=3 | 页面菜单：micro_name=`<biz>`、菜单名 + 路由地址（子应用内部路由，支持 `:param` 动态段）；首行 type=1 分组目录 path=`/sub/<biz>`；挂在哪个目录就渲染在哪，随菜单树参与角色授权与套餐裁剪 |
| sys_permission | 权限点（menu_id=NULL 登录即授予；挂到页面菜单后参与套餐裁剪） |

**方式 B（后台配置）**：平台端「平台管理 → 子应用管理」新增应用地址 +「菜单管理」登记类型=子应用页面的菜单（字段同上）。

**方式 C（本地开发/零配置兜底）**：mse-ui 的 `builtinMicroApps` 追加同构配置（登记式配置数据，允许的基座改动之一，见 conventions.md §2）。

多租户：**租户端子应用可用性由菜单推导**——套餐勾选该应用任一页面菜单即对该租户启用（页面级裁剪）；未登记页面菜单的应用对所有租户可用。

## 登录态与通信协议（模板已内置，业务无需重复实现）

- props 初始下发：`{ urlPrefix, token, tenantId, platform, nickname, permissions, theme }`
- bus 事件：`auth:change`（登录态广播）/ `theme:change`（外观广播：`{ dark, primary }`，主色衍生变量已按 Element Plus 官方算法整组计算）/ `sub:navigate`（基座菜单下发路径）/ `sub:ready`（子应用就绪上报）/ `sub:title`（路由切换上报 `meta.title`，基座页签与面包屑显示中文）/ `sub:auth-expired`（401 交基座登出）
- 按钮权限：`v-permission="'<biz>:xxx:yyy'"`（数组 = 任一命中；超管 `['*']` 直通）

## 核对清单

- [ ] dev 端口未与 alloc-tables 已用端口冲突
- [ ] package.json name / build base / router fallback / index.html 标题四处已参数化
- [ ] proxy 目标与后端形态一致（单体 8080 / 微服务网关 48080）
- [ ] 页面样式使用 Element Plus 变量（`var(--el-bg-color/--el-border-color/--el-text-color-*)`），不硬编码 `#fff/#f5f7fa` 等浅色值（深色模式下会白块刺眼）
- [ ] 基座注册完成且 menu 与 router 路由一致
- [ ] `npm install && npm run build` 通过；嵌入基座后登录态、菜单跳转、按钮权限正常
