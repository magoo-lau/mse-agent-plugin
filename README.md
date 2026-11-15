# zcode-plugin —— ai-mse 官方 ZCode 插件市场

把 mse 框架的工程组装规范与基座 API 使用方式沉淀为 ZCode 可直接调用的 skills，供第三方业务开发者（及 AI 助手）按规范创建业务工程、消费基座能力、生成标准代码。**本目录是开发者工具区，不参与 Maven/Nacos 构建。**

## 安装（本地目录市场，约 30 秒）

1. 打开 ZCode 客户端：**Settings → Plugin Management → Discover → `+`**；
2. 选择本目录（`zcode-plugin/`，内含 `marketplace.json`）添加为市场；
3. 安装市场中的 **mse-dev** 插件，默认启用即生效；
4. 卸载：同页面 Installed 标签移除即可，不影响仓库其他内容。

> 排障：若安装后技能未出现在会话中（本插件 skills 为三分类目录数组形式），先确认客户端版本支持 `plugin.json` 的 `skills` 数组字段；不支持时把三个分类目录下的 skill 文件夹平铺合并到一个 `skills/` 目录并改 `"skills": "skills"` 即可（skill 文件夹名已全局唯一，平铺无冲突）。

## 插件内容：mse-dev（14 个 skills）

| 分类 | Skill | 用途 |
| --- | --- | --- |
| 微服务 | `micro-create-project` | 创建微服务业务工程（xxx-api?/xxx-service（含 jar 内 db/<方言>/ 版本化迁移脚本，mse-target 头指令声明目标）/xxx-boot/nacos 留档），交互式收集业务名/包名/端口/权限号段 |
| 微服务 | `micro-gateway-route` | 在 mse-gateway.yaml 登记路由与免鉴权白名单 |
| 微服务 | `micro-nacos-config` | 服务专属配置留档 + init-configs.sh 导入 Nacos |
| 微服务 | `micro-deploy-artifacts` | 生成 Dockerfile 与 k8s 清单 |
| 单体 | `mono-create-project` | 创建单体聚合工程（mse.rpc.mode=local，默认仅需 MySQL/Redis；MQ 可选，与形态无关） |
| 单体 | `mono-aggregate-module` | 往现有单体聚合新业务积木 |
| 共享 | `mse-conventions` | 规范知识总览与 skill 路由（问答入口） |
| 共享 | `gen-crud` | 生成业务模块 CRUD 全套（entity/mapper/service/controller/权限点） |
| 共享 | `gen-rpc-contract` | 生成 RPC 契约（xxx-api + @RpcService + @RpcReference，local/dubbo 双形态） |
| 共享 | `gen-sql` | 版本化迁移脚本（db/<方言>/ + mse-target 头指令区分业务主库/基座库，自动应用免手工执行）+ 权限点 + 号段分配 |
| 共享 | `gen-ui-subapp` | 创建子前端工程并完成基座注册 |
| 共享 | `gen-ui-page` | 生成紧凑风格业务页面（v-permission 按钮权限） |
| 共享 | `gen-tenant-db` | 租户隔离接入（独立库/共享库分表/字段隔离，平台端界面化管理 + 数据源路由 + @GlobalTransactional 跨库事务） |
| 共享 | `review-conventions` | 按规范审查业务工程代码 |

skills 目录按形态分组：`skills/micro/`（微服务）、`skills/mono/`（单体）、`skills/shared/`（两形态通用）。

## 目录说明

```
zcode-plugin/
├── marketplace.json                  # 市场清单（+ 按钮添加本目录即识别）
└── plugins/mse-dev/
    ├── .zcode-plugin/plugin.json     # 插件清单
    ├── references/                   # 规范知识库（SKILL.md 按需引用）
    ├── templates/                    # 占位符模板：micro / mono / ui 三套
    ├── scripts/render.mjs            # 模板渲染脚本（占位符替换 + 条件块）
    └── skills/{micro,mono,shared}/   # 14 个 SKILL.md
```

模板是 `mse-sample/mse-demo`（微服务）、`mse-sample/mse-monolith`（单体）、`mse-sample/mse-ui-sub` + `demo-ui` 权限增强（前端）的去业务化精炼版，渲染即编译。底座演进时同步维护模板，并在 references/alloc-tables.md 登记新占用的端口与权限号段。
