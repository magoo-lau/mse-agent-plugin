package {{package}}.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import {{package}}.entity.{{Biz}}Item;
import {{package}}.mapper.{{Biz}}ItemMapper;
import {{package}}.service.{{Biz}}ItemService;

/**
 * {{Biz}} 示例条目（租户端 CRUD 参考实现）。
 *
 * <p>规范要点：权限码 {@code {{biz}}:item:*}（sys_permission 权限点在 jar 内 db/base 迁移脚本维护）；
 * Lambda 查询不拼字符串列名；取单行走分页插件按方言翻译（不用 LIMIT 1，信创库兼容）；
 * 统一 {@link Result} 包装；写操作业务逻辑放 service 层（双注解事务规范见 {{Biz}}ItemService）。
 */
@Validated
@RestController
@RequestMapping("/{{biz}}/item")
public class {{Biz}}ItemController {

    private final {{Biz}}ItemMapper itemMapper;
    private final {{Biz}}ItemService itemService;

    public {{Biz}}ItemController({{Biz}}ItemMapper itemMapper, {{Biz}}ItemService itemService) {
        this.itemMapper = itemMapper;
        this.itemService = itemService;
    }

    @SaCheckPermission(value = "{{biz}}:item:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated ItemSaveRequest request) {
        return Result.ok(itemService.create(request.name(), request.remark()));
    }

    @SaCheckPermission(value = "{{biz}}:item:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<{{Biz}}Item>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String name) {
        Page<{{Biz}}Item> page = itemMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<{{Biz}}Item>()
                        .like(name != null && !name.isBlank(), {{Biz}}Item::getName, name)
                        .orderByDesc({{Biz}}Item::getId));
        return Result.ok(PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords()));
    }

    @SaCheckPermission(value = "{{biz}}:item:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}")
    public Result<{{Biz}}Item> get(@PathVariable Long id) {
        return Result.ok(itemMapper.selectById(id));
    }

    @SaCheckPermission(value = "{{biz}}:item:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated ItemSaveRequest request) {
        {{Biz}}Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new ServiceException(1404, "条目不存在");
        }
        item.setName(request.name());
        item.setRemark(request.remark());
        itemMapper.updateById(item);
        return Result.ok();
    }

    @SaCheckPermission(value = "{{biz}}:item:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        itemMapper.deleteById(id);
        return Result.ok();
    }

    public record ItemSaveRequest(
            @NotBlank(message = "名称不能为空") String name,
            String remark) {
    }
}
