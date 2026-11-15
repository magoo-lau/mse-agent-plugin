import { createRouter, createWebHistory } from 'vue-router'

// 嵌入主应用时以主应用路由前缀为 base（由主应用 props 下发），独立运行时使用根路径
const embedded = !!window.__POWERED_BY_WUJIE__
const base = embedded ? window.$wujie?.props?.urlPrefix || '/sub/{{biz}}' : '/'

const router = createRouter({
  history: createWebHistory(base),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', component: () => import('../views/Home.vue'), meta: { title: '{{uiTitle}}' } }
    // 业务路由在此登记（gen-ui-page 生成的页面加到这里，并在基座「菜单管理」/业务 base.sql 登记类型=子应用页面的菜单，路由地址与 router 一致）
  ]
})

export default router
