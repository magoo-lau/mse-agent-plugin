package {{package}}.api;

import {{package}}.api.dto.{{Biz}}DTO;

/**
 * {{Biz}} 示例契约（{{biz}}-service 实现）。
 *
 * <p>业务工程自有契约：其他服务（或外部 Java 系统）依赖 {{biz}}-api 后经
 * {@code @RpcReference} 消费——单体 local 模式本地直调，微服务 Dubbo 模式远程调用，
 * 消费方代码零改动。租户上下文经 RPC 隐式透传，契约方法无需携带 tenantId。
 */
public interface Remote{{Biz}}Service {

    /** 按名称查询（当前租户域内）；不存在返回 null */
    {{Biz}}DTO getByName(String name);
}
