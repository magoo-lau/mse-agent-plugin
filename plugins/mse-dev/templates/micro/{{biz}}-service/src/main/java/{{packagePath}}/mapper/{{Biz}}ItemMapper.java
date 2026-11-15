package {{package}}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {{package}}.entity.{{Biz}}Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {{Biz}}ItemMapper extends BaseMapper<{{Biz}}Item> {
}
