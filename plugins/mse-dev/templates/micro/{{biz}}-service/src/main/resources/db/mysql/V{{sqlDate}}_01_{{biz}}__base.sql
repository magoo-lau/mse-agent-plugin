-- mse-target: base
SET NAMES utf8mb4;
-- =====================================================================
-- {{description}}基座接入脚本（独立工程 mse-{{biz}}，MySQL）
-- 目标库：租户库车队自持（mse-target: base；共享主库+各独立租户库各持 sys_menu/sys_permission/sys_micro_app 副本）
-- 内容：子应用注册（sys_micro_app）+ 页面菜单（sys_menu type=3）+ 权限点（sys_permission）
-- 配套：V{{sqlDate}}_01_{{biz}}__init.sql（mse-target: master：业务表 + undo_log）
-- 幂等：全段 NOT EXISTS / 条件 UPDATE 判重，重复推送无害
-- 执行：自动——随业务 jar 由 starter-bridge 推送 mse-system，车队重放到租户库按各库分册账本应用
--       （就绪即推 + 周期重推自愈，契约 RemoteBaseDataRegistry），无需手工执行；
--       管理端启停/改名等后续人工调整不被推送覆盖
-- 版本化：V{yyyyMMdd}_{NN}_<biz>__<描述>.sql（biz 段路由账本 mse_<biz>_schema_history，同 biz 内版本字典序即时间序）；
--         已应用脚本不再修改（改动报 checksum 冲突），变更一律新增版本文件
-- 号段：micro_app id={{appId}}、菜单 {{menu1}}-{{menu3}}、权限点 {{perm1}}+（登记见 references/alloc-tables.md）
-- 租户端可见性：页面菜单需在「套餐管理」勾选（页面级裁剪），普通用户另需角色授权
-- =====================================================================

-- ---------------- 子应用注册（按 name 判重） ----------------
INSERT INTO sys_micro_app (id, name, title, dev_entry, prod_entry, side, sort, status, remark, create_time, update_time)
SELECT {{appId}}, '{{biz}}', '{{uiTitle}}', '//localhost:{{uiPort}}', '/sub-apps/{{biz}}/', 1, 0, 0, '独立工程 mse-{{biz}}', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_micro_app WHERE name = '{{biz}}');

-- ---------------- 页面菜单（type=3；分组目录 path 即路由前缀 /sub/{{biz}}；按 id 判重） ----------------
-- 按业务需要在派生表中增删页面行（path 为子应用内部路由，支持 :param 动态段）
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, micro_name, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.micro_name, NULL, t.icon, t.sort, 0, 0, NOW(), NOW()
FROM (
    SELECT {{menu1}} AS id, 0 AS parent_id, '{{uiTitle}}' AS name, 1 AS type, 2 AS menu_scope, '/sub/{{biz}}' AS path, NULL AS micro_name, 'Box' AS icon, 0 AS sort
    UNION ALL SELECT {{menu2}}, {{menu1}}, '示例页面一', 3, 2, '/page-one', '{{biz}}', NULL, 1
    UNION ALL SELECT {{menu3}}, {{menu1}}, '示例页面二', 3, 2, '/page-two', '{{biz}}', NULL, 2
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

-- ---------------- 权限点（挂靠页面菜单：参与授权分组与套餐裁剪；menu_id=NULL 仅限登录即授予的通用权限） ----------------
INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, NOW(), NOW()
FROM (
    SELECT {{perm1}} AS id, {{menu2}} AS menu_id, '{{biz}}' AS module, '条目查询' AS name, '{{biz}}:item:list' AS code, 1 AS sort
    UNION ALL SELECT {{perm2}}, {{menu2}}, '{{biz}}', '条目创建', '{{biz}}:item:create', 2
    UNION ALL SELECT {{perm3}}, {{menu2}}, '{{biz}}', '条目修改', '{{biz}}:item:update', 3
    UNION ALL SELECT {{perm4}}, {{menu2}}, '{{biz}}', '条目删除', '{{biz}}:item:delete', 4
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);

-- ---------------- 升级兼容：早期安装的权限点未挂菜单（menu_id=NULL），此处幂等补挂 ----------------
UPDATE sys_permission SET menu_id = {{menu2}}, update_time = NOW() WHERE id IN ({{perm1}}, {{perm2}}, {{perm3}}, {{perm4}}) AND menu_id IS NULL;
