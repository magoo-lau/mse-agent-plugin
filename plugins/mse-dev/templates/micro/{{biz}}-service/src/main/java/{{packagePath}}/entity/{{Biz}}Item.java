package {{package}}.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {{Biz}} 示例条目（业务表）。
 *
 * <p>需要独立库租户路由时，加 {@code @TenantDatasource} 注解
 * （vip.mseapp.core.annotation.TenantDatasource）：租户×服务隔离配置（平台端「隔离策略」维护）命中
 * 独立库（或 sys_tenant.ds_key 全局兜底）
 * 时读写自动路由到租户专属数据源，否则留在共享主库；两种库的表结构一致，均含 tenant_id 列。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("{{biz}}_item")
public class {{Biz}}Item extends BaseEntity {

    /** 名称 */
    private String name;

    /** 备注 */
    private String remark;
}
