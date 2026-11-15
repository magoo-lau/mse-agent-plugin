---
name: gen-ui-page
description: >
  按 mse 紧凑型 UI 规范给子应用生成业务页面（列表页：筛选区 + 表格 + 分页 + 表单弹窗，
  v-permission 按钮权限，request.ts 调后端）并登记路由与基座菜单。当用户要写前端页面、
  列表页/详情页/表单、给子应用加功能页面时使用。
---

# gen-ui-page —— 子应用业务页面

参考实现：`mse-sample/mse-demo/demo-ui/src/views/Order.vue`。风格规范：`references/conventions.md` §9（紧凑型，信息密度优先）。

## 生成规则

1. **页面文件** `src/views/<Feature>.vue`（`<script setup lang="ts">` 组合式 API）：
   - 请求统一走 `src/api/request.ts`（返回体已剥壳：直接拿 data；业务错误 reject 携带 `{code,message}` 可分支处理）；API 路径与后端 Controller 一致（如 `/api` 已代理，实际写 `request.get('/<biz>/item/page', { params })`）。
   - **紧凑型**：全局 Element Plus `size: 'small'` 已内置——**不要逐组件写 size**；筛选区与表格紧凑排布（筛选行 `display:flex; gap:8px` + 检索/新建按钮同排），卡片留白收敛，个别控件确需大尺寸才局部覆盖。
   - 按钮权限：写操作按钮加 `v-permission="'<biz>:<资源>:<动作>'"`（与后端 @SaCheckPermission、sys_permission 同码）。
   - 分页：`el-pagination`（small），参数 pageNum/pageSize 与后端 PageResult 对齐（records/total）。
   - 删除/危险操作用 `ElMessageBox.confirm`；成功提示 `ElMessage.success`。
2. **路由登记** `src/router/index.ts`：懒加载 `{ path: '/<feature>', component: () => import('../views/<Feature>.vue'), meta: { title: '<中文名>' } }`——`meta.title` 经 App.vue 的 `sub:title` 上报基座，页签/面包屑显示中文（详情页等无菜单深链路由必须写）。
3. **页面菜单登记**：在基座「菜单管理」登记类型=子应用页面的菜单（或随业务 jar 内 `db/<方言>/V…_<biz>__base.sql` 版本化脚本（头指令 -- mse-target: base）幂等落库——starter-bridge 自动推送 mse-system 应用，免手工执行），路由地址=子应用内部路由（支持 `:param`），**与 router 登记保持一致**；`sys_micro_app`（子应用管理）与 mse-ui `builtinMicroApps` 兜底表只登记应用地址（无菜单字段，见 `references/conventions.md` §2），未登记页面菜单的应用由基座渲染单项兜底。
4. **页面菜单点击直达**：模板 App.vue 已订阅 `sub:navigate`，无需额外处理；动态参数路由 path 写 `:id` 形式（基座默认取 1）。

## 结构模板（列表页）

```
el-card
 ├─ 筛选行：输入框/下拉 + 查询 + 新建(v-permission)
 ├─ el-table(:data, border, 紧凑列宽) 
 ├─ el-pagination(small, total/page-size/current-page)
 └─ 新建/编辑 el-dialog(small 表单 + 校验) 
```

## 验证

`npm run build` 通过；`npm run dev` + 后端起服务后：查询分页正常、无权限账号看不到受控按钮、401 时经 `sub:auth-expired` 由基座登出。嵌入基座回归菜单直达与 alive 保活下重新登录后的 token 刷新。
