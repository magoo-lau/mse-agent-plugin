# mse-agent-plugin —— 插件文档索引

ZCode 插件市场仓（开发者工具区，不参与 Maven/npm 构建）。当前唯一插件 **mse-dev**：把 ai-mse 工程规范固化为 ZCode 技能，供第三方业务开发者以 AI 辅助方式按规范创建工程与写代码。

| 内容 | 位置 |
|---|---|
| 市场清单 | [../marketplace.json](../marketplace.json)（name: ai-mse，plugins: mse-dev，source: ./plugins/mse-dev） |
| 插件清单 | [../plugins/mse-dev/.zcode-plugin/plugin.json](../plugins/mse-dev/.zcode-plugin/plugin.json)（skills 三目录数组） |
| 技能 14 个 | `plugins/mse-dev/skills/micro/`（微服务 4：创建工程/网关路由/Nacos 配置/部署物）、`skills/mono/`（单体 2：创建聚合工程/聚合业务积木）、`skills/shared/`（通用 8：规范问答/CRUD/RPC 契约/三方言 SQL/子应用/页面/租户独立库/规范审查） |
| 规范知识 | `plugins/mse-dev/references/` 6 篇（conventions/micro-checklist/mono-checklist/fs-integration/**alloc-tables**/等）——**alloc-tables.md 是全体系端口/权限号段/子应用注册 id 的唯一登记处** |
| 模板 | `plugins/mse-dev/templates/{micro,mono,ui}/`——mse-demo / mse-monolith / mse-ui-sub 去业务化精炼版，占位符 `{{biz}}/{{uiPort}}` 等经 `scripts/render.mjs` 渲染 |
| 渲染脚本 | [../plugins/mse-dev/scripts/render.mjs](../plugins/mse-dev/scripts/render.mjs)：`node render.mjs <srcDir> <dstDir> --biz=... --port=...`（默认目标 mse-modules/mse-<biz>，模板 ui 的 file:../../../mse-ui-shared 在 2 层目标目录解析回仓库上层兄弟仓） |

## 拆仓说明

本仓自 ai-mse 单仓（源 commit `cc5410a3`）的 zcode-plugin/ 拆分独立。技能/references 内引用的 `mse-framework/...`、`mse-modules/...`、`mse-sample/...` 路径，在拆分后语义对应**兄弟仓库**（D:\docs\git\mse-framework 等）——按路径找文件时到对应仓找。安装方式：ZCode 客户端 Settings → Plugin Management → Discover 添加本目录为本地市场。

## 校验

CI 自动执行 JSON 清单解析 + render.mjs 三模板渲染冒烟（ui 模板需 `--ui-port/--ui-title/--proxy-target/--app-id`）；本仓无其他构建。
