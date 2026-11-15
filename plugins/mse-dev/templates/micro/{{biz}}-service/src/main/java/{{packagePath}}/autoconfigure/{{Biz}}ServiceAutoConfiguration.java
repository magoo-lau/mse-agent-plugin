package {{package}}.autoconfigure;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
{{#IF:API}}import {{package}}.api.Remote{{Biz}}Service;
import {{package}}.mapper.{{Biz}}ItemMapper;
import {{package}}.rpc.Remote{{Biz}}ServiceImpl;
{{/IF:API}}import vip.mseapp.core.annotation.TenantServiceModule;
import vip.mseapp.mybatis.config.MseMybatisAutoConfiguration;

/**
 * {{description}}——业务服务自动装配（Spring Boot SPI）。
 *
 * <p>接入方仅需引入 {{biz}}-service 依赖：Service / Mapper 均由此注册，
 * 无需 component-scan 业务包名，也无需 @MapperScan。控制器由
 * {@link {{Biz}}ControllerAutoConfiguration} 装配，两者均经
 * {@code META-INF/spring/...AutoConfiguration.imports} 登记。
 *
 * <p>RPC 契约实现采用 {@code @Bean + @ConditionalOnMissingBean} 显式注册：
 * 接入方声明自己的同契约 Bean 即可整体替换默认实现。
 *
 * <p>{@link TenantServiceModule @TenantServiceModule} 声明模块服务身份（服务名 + mapper 包根）：
 * 单体聚合接入时「租户隔离策略」的服务级配置按此服务名零配置生效（微服务形态即应用名，双形态一致）。
 */
@TenantServiceModule("{{appName}}")
@AutoConfiguration(before = MseMybatisAutoConfiguration.class)
@MapperScan(basePackages = "{{package}}.mapper", annotationClass = Mapper.class)
@ComponentScan(basePackages = "{{package}}.service")
public class {{Biz}}ServiceAutoConfiguration {
{{#IF:API}}
    @Bean
    @ConditionalOnMissingBean(Remote{{Biz}}Service.class)
    public Remote{{Biz}}Service remote{{Biz}}ServiceImpl({{Biz}}ItemMapper itemMapper) {
        return new Remote{{Biz}}ServiceImpl(itemMapper);
    }
{{/IF:API}}
}
