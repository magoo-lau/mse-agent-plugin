# 文件存储接入标准（mse-fs 优先、无 mse-fs 自动降级本地）

> 面向场景：业务模块需要文件上传/归档（附件、图片、素材、头像）时，**不要自造落盘逻辑**，按本文四步接入。
> 现成样板（读一个即可照抄）：`mse-modules/mse-oa/oa-form` 的 `OaFileStorage`（附件，行留 fsFileId）、
> `mse-framework/mse-service/service-platform` 的 `DomainAssetStorage`（品牌素材）、
> `mse-framework/mse-service/service-system` 的 `AvatarStorage`（用户头像）、
> `mse-modules/mse-ai/ai-service` 的 `KbStorageService`（知识库原文件归档）。
> 部署与 API 见 `mse-modules/mse-fs/README.md`；端口/页面资源占用见 `references/alloc-tables.md`。

## 一、接入语义

- **部署了 mse-fs**：文件统一进 mse-fs（微服务经 dubbo 远调，单体聚合本地直调），元数据/下载走 mse-fs 的 `fileId`；
- **未引入/未部署 mse-fs**：自动回落模块本地磁盘实现，业务代码零感知、启动不报错；
- 模块显式开关可强制（默认 `auto`）：`mse.<模块>.storage-mode = auto | fs | local`。

## 二、四步接入

1. **依赖**：模块 pom 引框架契约 `api-fs`（BOM 仲裁版本，仅接口 + DTO，无实现依赖）：

   ```xml
   <dependency>
       <groupId>vip.mseapp</groupId>
       <artifactId>api-fs</artifactId>
   </dependency>
   ```

2. **存储契约接口化**：模块内定义自己的存储接口 + 本地磁盘兜底实现：

   ```java
   public interface BizFileStorage {
       /** 保存文件，返回业务侧要落库的地址/句柄（实现决定形态） */
       String upload(MultipartFile file);
   }
   ```

3. **双嵌套条件装配（两侧条件必须互斥）**（自动降级语义的核心）：自动装配类加 `afterName`，
   放两个嵌套配置——Fs 实现以 `@Conditional(BizFsAvailableCondition.class)` 装配（内 `@RpcReference`），
   Local 兜底以 **`@Conditional(BizFsUnavailableCondition.class)`（取反）+ `@ConditionalOnMissingBean`** 双条件装配。
   ⚠️ 勿省略 Local 侧的取反条件、勿依赖嵌套类声明顺序兜底——javac 编译 InnerClasses 顺序不保序，
   Local 先注册时 Fs 会静默让位、附件静默落本地磁盘（样板见 oa-form `OaFileStorage`、service-platform `DomainAssetStorage`、
   service-system `AvatarStorage`、ai-service `KbStorageService`）：

   ```java
   @AutoConfiguration(before = MseMybatisAutoConfiguration.class,
           afterName = "vip.mseapp.fs.autoconfigure.FsServiceAutoConfiguration")
   public class BizServiceAutoConfiguration {

       @Configuration(proxyBeanMethods = false)
       static class FsBizFileStorageConfiguration {
           @Bean
           @Conditional(BizFsAvailableCondition.class)
           @ConditionalOnMissingBean(BizFileStorage.class)
           public BizFileStorage fsBizFileStorage() { return new FsBizFileStorage(); }
       }

       @Configuration(proxyBeanMethods = false)
       static class LocalBizFileStorageConfiguration {
           @Bean
           @Conditional(BizFsUnavailableCondition.class)   // 取反条件，与 Available 互斥
           @ConditionalOnMissingBean(BizFileStorage.class)
           public BizFileStorage bizFileStorage(@Value("${mse.biz.storage-dir:./biz-files}") String dir) {
               return new LocalBizFileStorage(dir);
           }
       }
   }
   ```

4. **元数据约定**：业务侧行留存自己的元数据并记录 `fsFileId`（或 `fs:{fileId}` 归档串）；
   下载/展示按行分流（行里有 fileId 走 mse-fs，否则走本地），切换存储模式后旧文件不丢。

## 三、Fs 实现样板（@RpcReference 注入契约）

```java
public class FsBizFileStorage implements BizFileStorage {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 小文件 base64（≤20MB）；弱链路适当加大超时 */
    @RpcReference(dubbo = @RpcReference.Dubbo(timeout = 60_000))
    private RemoteFileService remoteFileService;

    @Override
    public String upload(MultipartFile file) {
        FileUploadRequest request = new FileUploadRequest();
        request.setFileName(file.getOriginalFilename());
        request.setContentType(...); // 按扩展名白名单定型，不信任上传方声明
        request.setContentBase64(Base64.getEncoder().encodeToString(file.getBytes()));
        request.setBizType("biz-xxx");            // 业务归类标识
        request.setAccessLevel(FileAccessLevel.TENANT); // 默认租户级；公开展示素材用 PUBLIC
        FileMetaResult meta = JSON.readValue(
                remoteFileService.uploadFile(JSON.writeValueAsString(request)), FileMetaResult.class);
        return "fs:" + meta.getFileId();          // 归档串（或落 fsFileId 列）
    }
}
```

可用性条件类一对（照抄任一样板，改配置键即可；Local 兜底侧用取反条件）：

```java
public class BizFsAvailableCondition implements ConfigurationCondition {
    @Override public ConfigurationPhase getConfigurationPhase() { return ConfigurationPhase.REGISTER_BEAN; }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        if ("local".equalsIgnoreCase(env.getProperty("mse.biz.storage-mode", "auto"))) return false;
        if (!ClassUtils.isPresent("vip.mseapp.fs.api.RemoteFileService", getClass().getClassLoader())) return false;
        if ("fs".equalsIgnoreCase(env.getProperty("mse.biz.storage-mode", "auto"))) return true;
        if ("dubbo".equalsIgnoreCase(env.getProperty("mse.rpc.mode", "local"))) return true;
        // 单体 local 模式：仅当容器已装配 fs 契约实现（避免 @RpcReference 本地解析启动失败）
        var bf = context.getBeanFactory();
        return bf != null && bf.getBeanNamesForType(RemoteFileService.class, true, false).length > 0;
    }
}
```

## 四、公开素材通道（img 标签免登录展示）

`<img>` 不携带 Authorization token，需要免登录展示的素材（头像/登录页素材/公开附件预览）：

- **mse-fs 模式**：上传时 `accessLevel = FileAccessLevel.PUBLIC`，用 `FsAssetUrls.publicAssetUrl(TenantContext.getRequiredTenantId(), fileId, 原始文件名)`
  拼出 `/fs/asset/{tenantId}/{fileId}.{后缀}`（首段租户号=素材所属世界，0=平台世界）直接可作 `img src`
  （mse-fs 免登录端点按租户段路由寻址、按行校验，非 PUBLIC/跨世界 404；`/fs/asset/**` 已在网关 ignore-urls）；
- **本地兜底模式**：自备一个无鉴权注解的 GET 端点（`/{module}/asset/{name}`）读本地目录输出，
  并满足三点：① 落盘名白名单（`{雪花id}.{扩展名}`，防路径穿越）；② 输出带头
  `Content-Disposition: inline` + `X-Content-Type-Options: nosniff` + `Content-Security-Policy: default-src 'none'`（防 SVG 直开执行）+ 永久强缓存；③ **微服务形态把该路径加入 `mse-framework/docker/nacos/config/mse-gateway.yaml` 的 `mse.gateway.ignore-urls`**。

## 五、RPC 边界与注意

- base64 内容适合小文件（**≤20MB**），共享 `mse-micro-dubbo.yaml` 已将 dubbo payload 提至 32MB；更大文件走 mse-fs HTTP 端点/预签名直传；
- `@RpcReference` 必须标注在普通 `@Component`/Bean 字段，不能放 `@Configuration` 类（README 已知限制）；
- 校验白名单 + 大小上限放在 `validate` 一处，Fs/Local 两个实现共用同口径。

## 六、验收清单

- [ ] 未引入/未部署 mse-fs：启动正常，上传走本地实现不报错
- [ ] 部署 mse-fs 后：上传落 mse-fs（行留 fsFileId / `fs:{fileId}`），下载按行分流，旧文件不丢
- [ ] 公开素材 img 免登录可加载；非公开文件 404
- [ ] 显式 `storage-mode=local` 强制本地；`=fs` 强制 mse-fs
- [ ] 微服务形态新增的免登录路径已登记网关 ignore-urls
- [ ] `mvn install -DskipTests` 通过
