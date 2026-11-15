package {{package}}.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import {{package}}.api.Remote{{Biz}}Service;
import {{package}}.api.dto.{{Biz}}DTO;
import {{package}}.entity.{{Biz}}Item;
import {{package}}.mapper.{{Biz}}ItemMapper;
import vip.mseapp.core.annotation.RpcService;

import java.util.List;

/**
 * {{Biz}} 示例契约实现：单体 local 模式本地直调，微服务 Dubbo 模式自动导出（消费方零改动）。
 *
 * <p>注册方式：{{Biz}}ServiceAutoConfiguration 中 {@code @Bean + @ConditionalOnMissingBean}，
 * 接入方声明同契约 Bean 即可整体替换本默认实现。
 */
@RpcService(Remote{{Biz}}Service.class)
public class Remote{{Biz}}ServiceImpl implements Remote{{Biz}}Service {

    private final {{Biz}}ItemMapper itemMapper;

    public Remote{{Biz}}ServiceImpl({{Biz}}ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    @Override
    public {{Biz}}DTO getByName(String name) {
        List<{{Biz}}Item> records = itemMapper.selectList(
                new LambdaQueryWrapper<{{Biz}}Item>().eq({{Biz}}Item::getName, name));
        return records.isEmpty() ? null : toDto(records.get(0));
    }

    private static {{Biz}}DTO toDto({{Biz}}Item item) {
        {{Biz}}DTO dto = new {{Biz}}DTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setRemark(item.getRemark());
        dto.setTenantId(item.getTenantId());
        return dto;
    }
}
