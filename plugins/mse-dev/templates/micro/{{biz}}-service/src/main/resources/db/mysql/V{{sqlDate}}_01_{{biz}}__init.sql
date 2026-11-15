-- mse-target: master
SET NAMES utf8mb4;
-- =====================================================================
-- {{description}}业务表初始化脚本（独立工程 mse-{{biz}}，MySQL）
-- 目标库：业务库（mse-target: master；目标=业务主库+租户独立库；master 默认库=缺省共享租户库 mse_tenant）
-- 内容：业务表（{{biz}}_item 示例）+ Seata undo_log（启用 Seata 时必须存在于各写入库）
-- 配套：V{{sqlDate}}_01_{{biz}}__base.sql（mse-target: base：子应用注册 + 页面菜单 + 权限点）
--
-- 前置：无（基座库随基座应用启动自动初始化，业务主库首启自动建库；租户域 base 副本由车队重放就绪）
-- 执行：自动——随服务启动应用主库（mse.tenant.schema.auto-migrate 默认开启，失败阻断启动）；
--       租户独立库经平台端「租户数据源」界面初始化/懒初始化克隆+迁移自动就绪（本脚本不再含建库建表段）
-- 版本化：V{yyyyMMdd}_{NN}_<biz>__<描述>.sql（biz 段路由账本 mse_<biz>_schema_history，同 biz 内版本字典序即时间序）；
--         已应用脚本不再修改（改动报 checksum 冲突），变更一律新增版本文件；脚本保持幂等
-- =====================================================================

CREATE TABLE IF NOT EXISTS {{biz}}_item
(
    id          BIGINT        NOT NULL COMMENT '主键ID',
    tenant_id   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    name        VARCHAR(100)  NOT NULL COMMENT '名称',
    remark      VARCHAR(255)  NULL COMMENT '备注',
    create_by   BIGINT        NULL COMMENT '创建者',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT        NULL COMMENT '更新者',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id)
) ENGINE = InnoDB COMMENT ='{{biz}} 示例条目';

-- Seata undo_log（AT 模式按分支数据源记录回滚日志；独立库租户由克隆+迁移自动带上同构对象）
CREATE TABLE IF NOT EXISTS undo_log
(
    branch_id     BIGINT       NOT NULL COMMENT '分支事务 ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务 ID',
    context       VARCHAR(128) NOT NULL COMMENT '回滚上下文（序列化方式等）',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚数据快照（前后镜像）',
    log_status    INT          NOT NULL COMMENT '日志状态（0 正常 1 全局已完成）',
    log_created   DATETIME(6)  NOT NULL COMMENT '日志创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '日志修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB COMMENT ='Seata AT 模式回滚日志';

-- {{description}}权限点（{{perm1}}-{{perm4}}）在 V{{sqlDate}}_01_{{biz}}__base.sql（mse-target: base）
