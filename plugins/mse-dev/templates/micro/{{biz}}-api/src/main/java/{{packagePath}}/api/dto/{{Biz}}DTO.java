package {{package}}.api.dto;

import java.io.Serializable;

/**
 * {{Biz}} 示例条目（业务 RPC 契约）。契约 DTO 与实体解耦：只放跨服务需要的字段。
 */
public class {{Biz}}DTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String remark;
    /** 租户编号（条目归属租户；0 表示平台级） */
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
