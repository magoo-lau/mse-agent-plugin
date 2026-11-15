# 微服务业务工程文件清单（micro-checklist）

> 目标结构 = `mse-modules/mse-<biz>/`，蓝本 `mse-sample/mse-demo/`。`【P】`= 含参数化内容。模板位于插件根 `templates/micro/`，用 `scripts/render.mjs` 渲染生成。

## 模块结构

```
mse-modules/mse-<biz>/
├── pom.xml                          【P】父 pom：artifactId=mse-<biz>；parent 指向基座 mse-starter-parent（<relativePath/>，
│                                         随框架 mvn install 发布；收编 Java 21/编译与 boot 插件/BOM/lombok/test/仓库）；
│                                         modules 列 <biz>-api?/<biz>-service/<biz>-boot；dependencyManagement 仅管自有模块
├── README.md                        【P】工程文档（模块/端口/SQL/部署/验证）
├── <biz>-api/                       （可选，对外契约时保留）
│   ├── pom.xml                      【P】parent=mse-<biz>；仅依赖 common-core
│   └── src/main/java/vip/mseapp/<biz>/api/
│       ├── Remote<Biz>Service.java  【P】契约接口（javadoc 说明消费方式与租户上下文透传）
│       └── dto/<Biz>DTO.java        【P】implements Serializable，serialVersionUID=1L，getter/setter
├── <biz>-service/                   业务积木（纯 jar 无 main，SPI 自装配）
│   ├── pom.xml                      【P】依赖 <biz>-api? + starter-web/mybatis/security/rpc + api-tenant/api-system；
│                                         common-mq（工具类型）/starter-seata（@GlobalTransactional 编译期）按需（默认注释）
│   ├── src/main/resources/db/       【P】jar 内版本化迁移脚本（免手工执行，自动应用/自动推送）
│   │   ├── <方言>/V{yyyyMMdd}_{NN}_<biz>__init.sql   头指令 -- mse-target: master，目标=业务主库+租户独立库（业务表+undo_log）；
│   │   │                                              随启动 auto-migrate 应用（mse.tenant.schema.auto-migrate=true，失败阻断启动）
│   │   └── <方言>/V{yyyyMMdd}_{NN}_<biz>__base.sql   头指令 -- mse-target: base，目标=租户库车队自持（共享主库+各独立租户库；sys_micro_app 注册 + sys_menu type=3 页面菜单 +
│   │                                                      sys_permission 权限点，幂等）；starter-bridge 推送 mse-system 车队重放到租户库（各库自持副本）按账本应用
│   └── src/main/
│       ├── resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       │                            【P】登记 <Biz>ServiceAutoConfiguration + <Biz>ControllerAutoConfiguration 全类名
│       └── java/vip/mseapp/<biz>/
│           ├── autoconfigure/<Biz>ServiceAutoConfiguration.java    【P】见 conventions.md §3
│           ├── autoconfigure/<Biz>ControllerAutoConfiguration.java 【P】开关前缀 mse.service.<biz>.controller
│           ├── controller/<Biz>ItemController.java 【P】样例 CRUD；@SaCheckPermission(type=LOGIN_TYPE_TENANT)
│           ├── entity/<Biz>Item.java               【P】@TableName("<biz>_item") extends BaseEntity
│           ├── mapper/<Biz>ItemMapper.java         【P】@Mapper extends BaseMapper<T>
│           └── rpc/Remote<Biz>ServiceImpl.java     【P】@RpcService(契约.class)（仅保留 api 模块时）
├── <biz>-boot/                      微服务启动器
│   ├── pom.xml                      【P】micro-starter + <biz>-service（+ 按需 mq-starter）；
│                                         spring-boot-maven-plugin repackage
│   ├── src/main/java/vip/mseapp/boot/<biz>/<Biz>BootApplication.java 【P】纯 @SpringBootApplication
│   ├── src/main/resources/application.yaml 【P】name=mse-<biz>；spring.config.import 引 mse-micro-common/db/dubbo.yaml 与 {{appName}}.yaml（optional:nacos:）
│   ├── Dockerfile                   【P】ARG MODULE=<工程相对仓库根路径>/<biz>-boot、ARG PORT
│   └── k8s/<biz>.yaml               【P】Deployment+Service，name=mse-<biz>，containerPort
├── docker/nacos/config/mse-<biz>.yaml 【P】服务专属配置留档：port/management/可选 tenant2 数据源/可选 rocketmq/信创 profile；
│                                         无 Seata 时 spring.datasource.dynamic.seata: false
└── （无工程级 sql/ 目录——迁移脚本在 <biz>-service/src/main/resources/db/）
```

## 三方言 SQL 差异速查

版本化纪律：文件名 `V{yyyyMMdd}_{NN}_<biz>__<描述>.sql`（biz 段路由分册账本 `mse_<biz>_schema_history`；描述段含模块名，跨积木唯一；同日多件递增 `_02`）；账本按文件名去重；**已应用脚本不再修改**（checksum 冲突），变更一律新增版本文件；脚本保持幂等；免手工执行（init 随 auto-migrate、base 随推送自动应用）。

| 项 | MySQL | 达梦 DM8 | 金仓 KingbaseES |
| --- | --- | --- | --- |
| 库定位 | 脚本目标库由 mse-target 头指令决定（master 缺省主库=缺省共享租户库 mse_tenant），脚本内不写 USE | 同左 | 同左 |
| 时间列 | `DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE ...` | `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`（无 ON UPDATE） | 同达梦 |
| 布尔/删除标记 | `TINYINT` | `TINYINT` | `SMALLINT` |
| 注释 | 表内 `COMMENT ='...'` | `COMMENT ON TABLE/COLUMN` | `COMMENT ON TABLE/COLUMN` |
| 索引 | 表内 `KEY` | 表外 `CREATE INDEX` | 表外 `CREATE INDEX IF NOT EXISTS` |
| 权限点时间 | `NOW()` | `NOW()`（或 SYSDATE） | `NOW()` |
| 租户独立库 | 不写建库段——「租户数据源」界面初始化/懒初始化克隆+迁移自动就绪 | 同左 | 同左 |

## 生成后核对（红线路径）

- [ ] SPI imports 两个全类名与实际包名一致
- [ ] 迁移脚本位于 `<biz>-service/src/main/resources/db/<方言>/`，文件名符合 `V{yyyyMMdd}_{NN}_<biz>__<描述>.sql`、头指令 -- mse-target 声明目标且幂等
- [ ] k8s 清单（<biz>-boot/k8s/<biz>.yaml）含 MSE_ACTUATOR_TOKEN 环境变量、探针 exec+curl 带令牌头（X-Actuator-Token）
- [ ] 启动类在 `vip.mseapp.boot.<biz>`，纯 @SpringBootApplication
- [ ] 权限点号段与 alloc-tables 已登记一致，无冲突
- [ ] application.yaml 只含应用名 + spring.config.import（Nacos Data ID 清单），无数据源等业务配置
- [ ] 未修改 mse-framework 任何文件（mse-gateway.yaml 路由留档与 mse-ui `builtinMicroApps` 兜底登记两类登记式配置数据除外）
- [ ] `mvn install -DskipTests` 通过

## 基座侧联动（不属于新工程，必做）

1. `mse-framework/docker/nacos/config/mse-gateway.yaml` 加路由（见 micro-gateway-route skill）。
2. 权限点挂菜单时在平台端「菜单管理/权限点管理」登记（menu_id=NULL 免配置）。
3. 有子应用时执行 gen-ui-subapp（基座注册）。
4. 需要消费基座数据（用户/组织/租户）时按 `references/base-api.md` 以 `@RpcReference` 消费（micro-starter 已内置 starter-bridge）。
5. 需要文件存储（附件/图片/素材上传）时按 `references/fs-integration.md` 接入——mse-fs 优先、本地降级，勿自造落盘逻辑。
