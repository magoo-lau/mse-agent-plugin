---
name: gen-crud
description: >
  按 mse 框架规范给业务工程生成一个 CRUD 模块的全套代码：实体（BaseEntity/MyBatis-Plus）、
  Mapper、Service（双注解事务）、Controller（@SaCheckPermission + Lambda 查询 + Result 分页）
  与对应权限点 SQL。当用户要加表、加管理功能、写增删改查接口、生成业务模块时使用；
  微服务与单体形态通用。
---

# gen-crud —— 生成业务 CRUD 模块

参考实现：`mse-sample/mse-demo/demo-service`（订单 CRUD）与工程脚手架内样例 `XxxItemController/XxxItemService`。规范依据：`references/conventions.md` §3/§4/§8。

## 输入

确认/收集：目标业务工程与包名（vip.mseapp.\<biz\>）、表名（`<biz>_` 前缀）、实体与接口语义、权限码（`<biz>:<资源>:<动作>`）、权限点 id（查 alloc-tables 号段，不冲突）、是否需要逻辑删除外的特殊行为。

## 生成清单（每张表一套）

1. **实体** `entity/<Entity>.java`：`@Data @EqualsAndHashCode(callSuper = true) @TableName("<biz>_xxx")` 继承 `vip.mseapp.mybatis.data.BaseEntity`（自带 id/tenantId/审计字段/deleted，勿重复声明）；独立库租户表加 `@TenantDatasource`（见 gen-tenant-db）。
2. **Mapper** `mapper/<Entity>Mapper.java`：`@Mapper interface ... extends BaseMapper<T>`（无 XML）。
3. **Service** `service/<Entity>Service.java`：`@Service`；写方法一律 `@Transactional(rollbackFor = Exception.class)`；业务失败抛 `ServiceException(<业务码>, "<消息>")`。
4. **Controller** `controller/<Entity>Controller.java`：`@RestController @RequestMapping("/<biz>/<资源>")`；每个端点 `@SaCheckPermission(value = "<biz>:<资源>:<动作>", type = MseConstants.LOGIN_TYPE_TENANT)`；返回 `Result<T>` / `Result<PageResult<T>>`；分页 `selectPage(new Page<>(pageNum, pageSize), LambdaQueryWrapper)`，取单行用 `selectPage(Page.of(1,1,false), ...)`（不用 LIMIT 1，信创兼容）；请求体用 record + jakarta validation。
5. **权限点 SQL**：新表 DDL 写入 `<biz>-service/src/main/resources/db/<方言>/V{yyyyMMdd}_{NN}_<biz>__<描述>.sql` **新版本文件**（头指令 -- mse-target: master，目标=业务主库+租户独立库，auto-migrate 随启动自动应用；已应用脚本不再修改）；权限点写入 `db/<方言>/V…_<biz>__<描述>.sql` 新版本文件（头指令 -- mse-target: base，目标=租户库车队自持（共享主库+各独立租户库，starter-bridge 自动推送 mse-system 车队重放）），INSERT sys_permission **挂本工程页面菜单**（menu_id 指向 sys_menu type=3）；`menu_id=NULL` 仅限登录即授予的通用权限；脚本保持幂等；号段登记 alloc-tables。
6. **无需改 SPI/启动器**：新包在既有扫描范围内（controller/service/mapper 已由自动装配类覆盖）；全新子包种类（如 engine/support）才需同步修改 `<Biz>ServiceAutoConfiguration` 的 @ComponentScan。

## 红线

- 不写 mapper XML；不在 controller 写事务注解以外的事务编排（跨库见 gen-tenant-db）。
- 权限码与 SQL、前端 v-permission 三处一致。
- 单体与微服务代码同一套——不要写形态相关分支。

## 验证

`mvn install -DskipTests` 编译通过 → 启动（新增版本化脚本随 auto-migrate/推送自动应用，免手工执行）→ 用合法 token 走一遍 page/get/create/update/delete（权限 401/403 行为正确）。需要页面时接 gen-ui-page。
