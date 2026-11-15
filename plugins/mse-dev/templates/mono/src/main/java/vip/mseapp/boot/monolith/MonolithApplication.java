package vip.mseapp.boot.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {{description}}——单体启动器：聚合基座 service 与各业务积木，本地直调（mse.rpc.mode=local）。
 *
 * <p>全部 service 模块经 Spring Boot SPI（AutoConfiguration.imports）自装配：
 * 引入 jar 即得到全部接口与默认实现，启动类无需 scanBasePackages 覆盖积木包名，
 * 也无需 @MapperScan。各默认实现均带 @ConditionalOnMissingBean 回退，
 * 声明同契约 Bean 即可整体替换（如自定义 SysUserService）。
 *
 * <p>默认仅需 MySQL + Redis 即可运行，无 Nacos/Dubbo/Seata 依赖——
 * 开发与轻量部署零中间件成本；业务内 @RpcReference 契约消费在 local 模式下本地直调。
 * MQ 与形态正交：需要 RocketMQ 时放开 application.yaml 的 rocketmq 段（pom 补 mq-starter）即可，不配则事件降级 noop。
 */
@SpringBootApplication
public class MonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
