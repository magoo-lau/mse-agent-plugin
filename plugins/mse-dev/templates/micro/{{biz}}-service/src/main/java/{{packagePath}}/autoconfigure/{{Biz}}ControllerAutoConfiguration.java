package {{package}}.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * {{Biz}} 控制器自动装配（Spring Boot SPI）。
 *
 * <p>仅在 Servlet Web 环境注册；可用 {@code mse.service.{{biz}}.controller.enabled=false}
 * 整体关闭默认控制器，由接入方自有控制器替代（基座同款开关语义）。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "mse.service.{{biz}}.controller", name = "enabled", matchIfMissing = true)
@ComponentScan(basePackages = "{{package}}.controller")
public class {{Biz}}ControllerAutoConfiguration {
}
