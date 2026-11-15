package {{package}}.service;

import {{package}}.entity.{{Biz}}Item;
import {{package}}.mapper.{{Biz}}ItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {{Biz}} 条目业务逻辑。
 *
 * <p>事务注解规范（双注解）：写操作一律 {@code @Transactional(rollbackFor = Exception.class)}
 * （单体单库形态的事务保障 + 微服务形态的全局分支）；跨服务/跨库全局入口再叠加
 * {@code @GlobalTransactional}。例外：一个方法内跨多库路由（写主库 + 租户库）
 * 禁止 {@code @Transactional}（连接绑定先于路由决策），只用 {@code @GlobalTransactional}。
 */
@Service
public class {{Biz}}ItemService {

    private final {{Biz}}ItemMapper itemMapper;

    public {{Biz}}ItemService({{Biz}}ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(String name, String remark) {
        {{Biz}}Item item = new {{Biz}}Item();
        item.setName(name);
        item.setRemark(remark);
        itemMapper.insert(item);
        return item.getId();
    }
}
