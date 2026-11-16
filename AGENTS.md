# AGENTS.md —— mse-agent-plugin（ZCode 插件 mse-dev）协作约定

本文件面向所有在本仓库工作的 AI 助手与开发者。本仓库自 ai-mse 单仓（源 commit `cc5410a3`）的 zcode-plugin/ 拆分独立，是开发者工具区（不参与 Maven/Nacos/npm 构建）。

## 一、定位

把 ai-mse 工程规范固化为 ZCode 插件 **mse-dev**：skills 按 `skills/micro/`（微服务）、`skills/mono/`（单体）、`skills/shared/`（通用）三目录组织，共 14 个；规范知识沉淀在 `references/`；**模板即规范**——`templates/{micro,mono,ui}/` 是 mse-demo / mse-monolith / mse-ui-sub 的去业务化精炼版。

## 二、关键约定（红线）

1. **资源登记唯一真源**：端口 / 权限号段 / 子应用注册 id / 页面菜单号段 / 网关前缀 / 闸门键，一律以本仓 `plugins/mse-dev/references/alloc-tables.md` 为准——新增占用必须先查后用、用后登记。
2. **模板即规范**：改 templates/ 时保持与上游参考实现同源语义（micro←mse-demo 仓、mono←mse-monolith 仓、ui←mse-ui-sub 模板）；占位符只能经 `scripts/render.mjs` 渲染，模板本身不可直接构建。
3. **同步纪律**：底座能力演进（新 starter、SPI 结构、配置基线变化）时同步更新对应模板与 references；底座改动在 mse-framework 仓发生，本仓跟进。
4. **路径语义**：技能/references 内的 `mse-framework/...`、`mse-modules/...`、`mse-sample/...` 在拆分后指兄弟仓库；新文档一律写「兄弟仓 + 相对路径」口径。
5. **插件形态**：`plugins/mse-dev/.zcode-plugin/plugin.json` 的 skills 以三目录数组登记；marketplace.json 的 name 字段保留 `ai-mse`（历史口径，更名单独决策）。

## 三、校验与验证

- `node plugins/mse-dev/scripts/render.mjs <srcDir> <dstDir> --biz=demo --port=xxxx` 渲染冒烟（CI 自动跑 JSON 解析 + 渲染）。
- 模板改动验证：渲染到临时目录 → 在渲染产物内 `mvn validate` / `npm run build`（依赖兄弟仓 mse-framework 已 install）。

## 四、提交纪律

模板改动与 references 改动分开提交；新增技能同步 plugin.json 与本文件清单。
