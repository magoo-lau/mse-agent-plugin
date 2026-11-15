#!/usr/bin/env node
/**
 * render.mjs —— mse-dev 插件模板渲染脚本
 *
 * 用法：
 *   node render.mjs <srcDir> <dstDir> [--biz=mp] [--port=8098] [--no-api] [更多参数见下]
 *
 * 占位符（严格模式：仅替换下表已知 token，写法固定为 {{token}}，不带空格；
 * Vue 插值请始终写 "{{ x }}" 带空格，避免被误替换）：
 *   {{biz}} {{Biz}} {{package}} {{packagePath}} {{appName}} {{moduleDir}} {{modulePath}}
 *   {{gatewayPrefix}} {{mseVersion}} {{description}} {{port}} {{uiPort}} {{uiTitle}}
 *   {{proxyTarget}} {{perm1}} {{perm2}} {{perm3}} {{perm4}}
 *   {{appId}} —— sys_micro_app 注册 id；{{menu1}} {{menu2}} {{menu3}} —— 基座接入 SQL 的菜单 id
 *   （menu1=分组目录，menu2/menu3=示例页面；号段先查 references/alloc-tables.md 并登记）
 *   {{sqlDate}} —— 版本化迁移脚本日期段（V{yyyyMMdd}_NN_<biz>__… 文件名），默认渲染当天，
 *   可用 --sql-date 覆盖；脚本位于 <biz>-service/src/main/resources/db/<方言>/（目标由脚本头 -- mse-target: 指令声明）
 *
 * 条件块（块内内容按 flag 保留/删除，token 风格 {{#IF:FLAG}}...{{/IF:FLAG}}）：
 *   API —— 默认开启；--no-api 时删除条件块，并跳过路径含 api/rpc 目录段的文件
 *     （即整个 <biz>-api 契约模块与 service 内 rpc 实现包不生成）
 *
 * 自动派生（未显式传入时）：
 *   Biz=PascalCase(biz)  package=vip.mseapp.<biz>  packagePath=package 的 / 形式
 *   appName=mse-<biz>  moduleDir=mse-modules/mse-<biz>  modulePath=<moduleDir>/<biz>-boot
 *   gatewayPrefix=/<biz>  mseVersion=1.0.0-SNAPSHOT  description=<biz> 业务
 *
 * 结束时校验：输出目录中不允许残留 {{#IF: 未处理块；出现已知 token 但值为空 → 报错退出。
 */

import fs from 'node:fs'
import path from 'node:path'

function pascal(name) {
  return name.split(/[-_]/).filter(Boolean)
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join('')
}

function parseArgs(argv) {
  const opts = {}
  for (const arg of argv) {
    if (!arg.startsWith('--')) continue
    const eq = arg.indexOf('=')
    if (eq < 0) { opts[arg.slice(2)] = true; continue }
    opts[arg.slice(2, eq)] = arg.slice(eq + 1)
  }
  return opts
}

const args = parseArgs(process.argv.slice(2))
const [srcArg, dstArg] = process.argv.slice(2).filter((a) => !a.startsWith('--'))
if (!srcArg || !dstArg) {
  console.error('用法: node render.mjs <srcDir> <dstDir> --biz=mp [--port=8098] [--no-api] ...')
  process.exit(1)
}
const srcDir = path.resolve(srcArg)
const dstDir = path.resolve(dstArg)
if (!fs.existsSync(srcDir) || !fs.statSync(srcDir).isDirectory()) {
  console.error(`模板目录不存在: ${srcDir}`)
  process.exit(1)
}

// ---- 参数 → token 表 ----
const biz = args.biz
if (!biz || !/^[a-z][a-z0-9-]*$/.test(biz)) {
  console.error('缺少或非法 --biz（小写字母/数字/连字符，如 mp、oa-form）')
  process.exit(1)
}
const pkg = args.package || `vip.mseapp.${biz}`
// 版本化迁移脚本日期段（V{yyyyMMdd}_NN_<biz>__<描述>.sql），默认渲染当天
const now = new Date()
const sqlDate = args['sql-date'] ||
  `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
const tokens = {
  biz,
  Biz: pascal(biz),
  package: pkg,
  packagePath: pkg.split('.').join('/'),
  appName: args['app-name'] || `mse-${biz}`,
  moduleDir: args['module-dir'] || `mse-modules/mse-${biz}`,
  gatewayPrefix: args['gateway-prefix'] || `/${biz}`,
  mseVersion: args['mse-version'] || '1.0.0-SNAPSHOT',
  description: args.description || `${biz} 业务`,
  port: args.port,
  uiPort: args['ui-port'],
  uiTitle: args['ui-title'],
  proxyTarget: args['proxy-target'],
  modulePath: args['module-path'],
  perm1: args.perm1, perm2: args.perm2, perm3: args.perm3, perm4: args.perm4,
  appId: args['app-id'],
  menu1: args.menu1, menu2: args.menu2, menu3: args.menu3,
  sqlDate
}
if (!tokens.modulePath) tokens.modulePath = `${tokens.moduleDir}/${biz}-boot`

// ---- 条件块 flags ----
const flags = new Set(['API'])
if (args['no-api']) flags.delete('API')
// --no-api 时跳过的目录段：契约模块（模板内为原始 token 目录名，渲染后为 <biz>-api）与 rpc 实现包
const SKIP_SEGMENTS_NO_API = new Set([`${biz}-api`, '{{biz}}-api', 'api', 'rpc'])

const IF_OPEN = /\{\{#IF:([A-Z0-9_-]+)\}\}/
const IF_CLOSE = (f) => new RegExp(`\\{\\{/IF:${f}\\}\\}`)

/** 处理条件块：flag 开 → 保留内容去标记；关 → 整块删除 */
function applyIfBlocks(text) {
  let out = text
  let guard = 0
  while (guard++ < 100) {
    const open = out.match(IF_OPEN)
    if (!open) break
    const flag = open[1]
    const rest = out.slice(open.index + open[0].length)
    const close = rest.match(IF_CLOSE(flag))
    if (!close) throw new Error(`条件块未闭合: {{#IF:${flag}}}（附近: ...${out.slice(Math.max(0, open.index - 30), open.index + 50)}...）`)
    const inner = rest.slice(0, close.index)
    out = out.slice(0, open.index) + (flags.has(flag) ? inner : '') + rest.slice(close.index + close[0].length)
  }
  if (IF_OPEN.test(out)) throw new Error('存在未处理的条件块标记')
  return out
}

/** 严格 token 替换：仅替换已知 token；值为 undefined 的已知 token 视为缺参报错 */
function applyTokens(text, where) {
  return text.replace(/\{\{([a-zA-Z0-9_]+)\}\}/g, (m, name) => {
    if (!(name in tokens)) return m // 未知 token（如 Vue 插值）原样保留
    const v = tokens[name]
    if (v === undefined || v === null || v === '') {
      throw new Error(`文件 ${where} 使用了 {{${name}}} 但未提供对应参数（--${name.replace(/[A-Z]/g, (c) => '-' + c.toLowerCase())}）`)
    }
    return String(v)
  })
}

const written = []
function walk(dir, relBase) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const abs = path.join(dir, entry.name)
    const rel = relBase ? `${relBase}/${entry.name}` : entry.name
    if (entry.isDirectory()) { walk(abs, rel); continue }
    if (!entry.isFile()) continue
    if (!flags.has('API') && rel.split('/').some((seg) => SKIP_SEGMENTS_NO_API.has(seg))) continue

    const relOut = applyTokens(rel, rel)
    const content = applyTokens(applyIfBlocks(fs.readFileSync(abs, 'utf8')), rel)
    if (/\{\{#IF:/.test(content)) throw new Error(`${rel}: 残留未处理条件块`)

    const outAbs = path.join(dstDir, relOut)
    fs.mkdirSync(path.dirname(outAbs), { recursive: true })
    fs.writeFileSync(outAbs, content)
    written.push(relOut)
  }
}

walk(srcDir, '')
console.log(`已渲染 ${written.length} 个文件 → ${dstDir}  (flags: ${[...flags].join(',') || '无'})`)
for (const f of written) console.log('  ' + f)
